/**
 * 后台公共脚本
 * 集中提供时间格式化等通用方法，避免在各页面重复定义。
 */

/**
 * 将时间戳（毫秒）格式化为 yyyy-MM-dd HH:mm:ss；0 或空值显示为“无”。
 */
function formatDate(now) {
    if (now === undefined || now === null || now === "" || Number(now) === 0) {
        return "无";
    }
    var date = new Date(Number(now));
    function pad(n) {
        return n > 9 ? n : '0' + n;
    }
    return date.getFullYear()
        + "-" + pad(date.getMonth() + 1)
        + "-" + pad(date.getDate())
        + " " + pad(date.getHours())
        + ":" + pad(date.getMinutes())
        + ":" + pad(date.getSeconds());
}
