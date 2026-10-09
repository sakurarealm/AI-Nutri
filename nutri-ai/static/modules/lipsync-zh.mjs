/**
* @class 中文唇形同步处理器
* @author Yoyo 
* Email：1294858802@qq.com
*/
/**
 * pinyin 是实际应用中需要使用实际的拼音转换实现
 * npm i pinyin 或者 https://cdn.jsdelivr.net/npm/pinyin@4.0.0-alpha.2/+esm
 */
import pinyin from 'https://cdn.jsdelivr.net/npm/pinyin@4.0.0-alpha.2/+esm';

class LipsyncZh {

  /**
  * @constructor
  */
  constructor() {
    // 拼音到viseme的映射
    this.visemes = {
      // 韵母映射
      'a': 'aa', 'o': 'O', 'e': 'E', 'ê': 'E',
      'ai': 'aa', 'ei': 'E', 'ao': 'aa', 'ou': 'O',
      'an': 'aa', 'en': 'E', 'ang': 'aa', 'eng': 'E',
      'er': 'RR', 'i': 'I', 'ia': 'aa', 'ie': 'E',
      'iao': 'I', 'iu': 'I', 'ian': 'I', 'in': 'I',
      'iang': 'aa', 'ing': 'I', 'u': 'U', 'ü': 'U',
      'ua': 'aa', 'uo': 'O', 'uai': 'aa', 'ui': 'U',
      'uan': 'aa', 'un': 'U', 'uang': 'aa', 'ong': 'O',

      // 声母映射
      'b': 'PP', 'p': 'PP', 'm': 'PP', 'f': 'FF',
      'd': 'DD', 't': 'DD', 'n': 'nn', 'l': 'nn',
      'g': 'kk', 'k': 'kk', 'h': 'kk',
      'j': 'SS', 'q': 'SS', 'x': 'SS',
      'zh': 'SS', 'ch': 'SS', 'sh': 'SS', 'r': 'RR',
      'z': 'SS', 'c': 'SS', 's': 'SS'
    };

    // Viseme相对持续时间
    this.visemeDurations = {
      'aa': 1.2, 'E': 1.0, 'I': 0.8, 'O': 1.0,
      'U': 0.9, 'PP': 0.7, 'SS': 1.0, 'DD': 0.8,
      'FF': 0.9, 'kk': 0.7, 'nn': 0.8, 'RR': 0.8,
      'sil': 1
    };

    // 特殊符号处理
    this.symbols = {
      '%': '百分之', '€': '欧元', '$': '美元','￥':'人民币','@':'艾特',
      '℃': '摄氏度', 'm²': '平方米'
    };
    this.symbolsReg = /[%€$℃²￥@]/g;

    // 中文数字系统
    this.digits = ['零', '一', '二', '三', '四', '五', '六', '七', '八', '九'];
    this.units = ['', '十', '百', '千', '万', '十', '百', '千', '亿'];
  }

  /**
  * 数字转中文
  * @param {string} num 数字字符串
  * @return {string} 中文数字
  */
  numberToChinese(num) {
    let result = [];
    const parts = num.split('.');
    const integer = parts[0];
    const decimal = parts[1];

    // 处理负数
    if (integer.startsWith('-')) {
      result.push('负');
      this.convertIntegerPart(integer.slice(1), result);
    } else {
      this.convertIntegerPart(integer, result);
    }

    // 处理小数
    if (decimal) {
      result.push('点');
      [...decimal].forEach(d => {
        result.push(this.digits[parseInt(d)]);
      });
    }

    return result.join('');
  }

  /**
  * 文本预处理
  * @param {string} s 原始文本
  * @return {string} 处理后的文本
  */
  preProcessText(s) {
    return s
      .normalize('NFKC') // 先做Unicode正规化
      .replace(/[#_*\'\"：；]/g, '') // 移除不需要的符号（添加中文冒号）
      // 优先处理带符号的复合数字
      .replace(/(-?\d+(?:\.\d+)?)%/g, (_, num) => {
        return `百分之${this.numberToChinese(num)}`
      })
      .replace(/(-?\d+(?:\.\d+)?)([€$¥])/g, (_, num, symbol) => {
        return `${this.symbols[symbol] || ''}${this.numberToChinese(num)}`
      })
      // 处理温度单位（支持负温）
      .replace(/(-?\d+(?:\.\d+)?)℃/g, (_, num) => {
        return `${this.numberToChinese(num)}摄氏度`
      })
      // 处理其他预定义符号
      .replace(this.symbolsReg, (symbol) => this.symbols[symbol] || symbol)
      // 处理剩余独立数字（含负数、小数）
      .replace(/(-?\d+(?:\.\d+)?)/g, m => this.numberToChinese(m))
      // 统一处理标点（增加中文符号）
      .replace(/[，。！]/g, ' $& ') // 在标点符号前后添加空格
      .replace(/\s+/g, ' ')
      .trim();
  }

  /**
* 转换整数部分为中文数字
* @param {string} numStr 整数部分字符串
* @param {Array} result 结果存储数组
*/
  convertIntegerPart(numStr, result) {
    const digits = ['', '一', '二', '三', '四', '五', '六', '七', '八', '九'];
    const units = ['', '十', '百', '千'];
    const bigUnits = ['', '万', '亿', '兆']; // 扩展支持大数位

    // 特殊情况：纯零
    if (/^0+$/.test(numStr)) {
      result.push('零');
      return;
    }

    const length = numStr.length;
    let zeroFlag = false; // 标记是否需要补零
    let hasValue = false; // 标记当前节是否有有效值

    // 从低位到高位每4位分节处理
    for (let i = 0; i < length; i++) {
      const digit = parseInt(numStr[length - 1 - i]); // 当前位数
      const pos = i % 4; // 节内位置
      const section = Math.floor(i / 4); // 当前节数

      // 处理节单位（万/亿）
      if (pos === 0 && section > 0) {
        if (hasValue || bigUnits[section] === '亿') { // 特殊处理亿单位
          result.unshift(bigUnits[section]);
        }
        hasValue = false;
      }

      // 处理零
      if (digit === 0) {
        zeroFlag = true;
      } else {
        // 需要补零的情况：非连续零且前面有数字
        if (zeroFlag && hasValue) {
          result.unshift('零');
          zeroFlag = false;
        }

        // 添加单位和数字
        if (pos > 0) result.unshift(units[pos]);
        result.unshift(digits[digit]);
        hasValue = true;
      }

      // 处理最高位单位（例如：十二万）
      if (i === length - 1 && digit === 1 && pos === 1) {
        if (result[0] === '一十') result.shift();
      }
    }

    // 处理末尾单位
    if (bigUnits[Math.floor((length - 1) / 4)] && hasValue) {
      result.unshift(bigUnits[Math.floor((length - 1) / 4)]);
    }
  }

  /**
  * 转换中文到viseme序列
  * @param {string} w 处理后的文本
  * @return {Object} 唇形数据
  */
  wordsToVisemes(w) {
    const output = { visemes: [], times: [], durations: [] };
    let time = 0;

    // 分割文本，保留标点符号
    const segments = w.split(/([，。！])/).filter(Boolean);

    for (const segment of segments) {
      // // 如果是标点符号
      // if (/[，。！]/.test(segment)) {
      //   // 添加停顿
      //   this.addViseme(output, 'sil', time);
      //   time += this.visemeDurations['sil'] * 1.5; // 标点符号停顿时间更长
      //   continue;
      // }

      // 处理普通文本
      const pinyins = this.chineseToPinyin(segment);
      for (let pinyin of pinyins) {
        const [initial, final] = this.splitPinyin(pinyin);

        // 处理声母
        if (initial) {
          const v = this.visemes[initial];
          if (v) {
            this.addViseme(output, v, time);
            time += this.visemeDurations[v] || 1;
          }
        }

        // 处理韵母
        if (final) {
          const v = this.visemes[final];
          if (v) {
            this.addViseme(output, v, time);
            time += this.visemeDurations[v] || 1;
          }
        }

        // 添加间隔
        time += 0.2;
      }
    }

    return output;
  }

  // 辅助方法：添加viseme并合并连续相同项
  addViseme(output, viseme, time) {
    const last = output.visemes.length - 1;
    if (last >= 0 && output.visemes[last] === viseme) {
      output.durations[last] += this.visemeDurations[viseme] || 1;
    } else {
      output.visemes.push(viseme);
      output.times.push(time);
      output.durations.push(this.visemeDurations[viseme] || 1);
    }
  }

  // 示例拼音转换
  chineseToPinyin(text) {
    return pinyin(text, {
      style: pinyin.STYLE_NORMAL, // 不带声调
      heteronym: false           // 不启用多音字
    }).map(arr => arr[0]);       // 取第一个读音
  }

  splitPinyin(pinyin) {
    // 处理特殊整体认读音节
    const fullSpellMap = {
      'zhi': ['zh', 'i'], 'chi': ['ch', 'i'], 'shi': ['sh', 'i'],
      'ri': ['r', 'i'], 'zi': ['z', 'i'], 'ci': ['c', 'i'],
      'si': ['s', 'i'], 'yi': ['', 'i'], 'wu': ['', 'u'],
      'yu': ['', 'ü'], 'ye': ['', 'ie'], 'yue': ['', 'üe'],
      'yin': ['', 'in'], 'yun': ['', 'ün'], 'yuan': ['', 'üan'],
      'ying': ['', 'ing']
    };
    if (fullSpellMap[pinyin]) return fullSpellMap[pinyin];

    // 处理以y/w开头的零声母
    if (pinyin.startsWith('y') || pinyin.startsWith('w')) {
      return this.handleZeroInitial(pinyin);
    }

    // 按声母优先级检查（先检查复合声母）
    const initials = [
      'zh', 'ch', 'sh', // 复合声母优先
      'b', 'p', 'm', 'f', 'd', 't', 'n', 'l',
      'g', 'k', 'h', 'j', 'q', 'x',
      'r', 'z', 'c', 's'
    ];

    for (const init of initials) {
      if (pinyin.startsWith(init)) {
        const final = pinyin.slice(init.length);
        // 处理ü的特殊情况
        if (final.startsWith('v')) {
          return [init, final.replace('v', 'ü')];
        }
        return [init, final];
      }
    }

    // 处理零声母情况
    return ['', this.handleZeroFinal(pinyin)];
  }

  // 处理零声母开头的情况
  handleZeroInitial(pinyin) {
    const mapping = {
      'ya': 'ia', 'yan': 'ian', 'yang': 'iang', 'yao': 'iao',
      'yo': 'io', 'yong': 'iong', 'you': 'iou',
      'wa': 'ua', 'wai': 'uai', 'wan': 'uan', 'wang': 'uang',
      'weng': 'ueng', 'wo': 'uo', 'wei': 'uei'
    };

    if (mapping[pinyin]) {
      return ['', mapping[pinyin]];
    }

    // 处理y开头的特殊转换
    if (pinyin.startsWith('y')) {
      return ['', pinyin.replace('y', 'i').replace('v', 'ü')];
    }

    // 处理w开头的特殊转换
    if (pinyin.startsWith('w')) {
      return ['', pinyin.replace('w', 'u')];
    }

    return ['', pinyin];
  }

  // 处理零声母的韵母修正
  handleZeroFinal(final) {
    const replacements = {
      'iu': 'iou', 'ui': 'uei', 'un': 'uen',
      'v': 'ü', 've': 'üe', 'vn': 'ün'
    };
    return replacements[final] || final;
  }

}

export { LipsyncZh };