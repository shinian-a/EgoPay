var userAgent = navigator.userAgent.toLowerCase();
function aaa() {
        if ($('#orderDetail').hasClass('detail-open')) {
            $('#orderDetail .detail-ct').slideUp(500, function () {
                $('#orderDetail').removeClass('detail-open');
            });
        } else {
            $('#orderDetail .detail-ct').slideDown(500, function () {
                $('#orderDetail').addClass('detail-open');
            });
        }
    }
    function formatDate(now) {
        now = new Date(now*1000)
        return now.getFullYear()
            + "-" + (now.getMonth()>8?(now.getMonth()+1):"0"+(now.getMonth()+1))
            + "-" + (now.getDate()>9?now.getDate():"0"+now.getDate())
            + " " + (now.getHours()>9?now.getHours():"0"+now.getHours())
            + ":" + (now.getMinutes()>9?now.getMinutes():"0"+now.getMinutes())
            + ":" + (now.getSeconds()>9?now.getSeconds():"0"+now.getSeconds());

    }
    var myTimer;
    function timer(intDiff) {
        var i = 0;
        i++;
        var day = 0,
            hour = 0,
            minute = 0,
            second = 0;//时间默认值
        if (intDiff > 0) {
            day = Math.floor(intDiff / (60 * 60 * 24));
            hour = Math.floor(intDiff / (60 * 60)) - (day * 24);
            minute = Math.floor(intDiff / 60) - (day * 24 * 60) - (hour * 60);
            second = Math.floor(intDiff) - (day * 24 * 60 * 60) - (hour * 60 * 60) - (minute * 60);
        }
        if (minute <= 9) minute = '0' + minute;
        if (second <= 9) second = '0' + second;
        $('#hour_show').html('<s id="h"></s>' + hour + '时');
        $('#minute_show').html('<s></s>' + minute + '分');
        $('#second_show').html('<s></s>' + second + '秒');
        if (hour <= 0 && minute <= 0 && second <= 0) {
            qrcode_timeout()
            clearInterval(myTimer);

        }
        intDiff--;

        myTimer = window.setInterval(function () {
            i++;
            var day = 0,
                hour = 0,
                minute = 0,
                second = 0;//时间默认值
            if (intDiff > 0) {
                day = Math.floor(intDiff / (60 * 60 * 24));
                hour = Math.floor(intDiff / (60 * 60)) - (day * 24);
                minute = Math.floor(intDiff / 60) - (day * 24 * 60) - (hour * 60);
                second = Math.floor(intDiff) - (day * 24 * 60 * 60) - (hour * 60 * 60) - (minute * 60);
            }
            if (minute <= 9) minute = '0' + minute;
            if (second <= 9) second = '0' + second;
            $('#hour_show').html('<s id="h"></s>' + hour + '时');
            $('#minute_show').html('<s></s>' + minute + '分');
            $('#second_show').html('<s></s>' + second + '秒');
            if (hour <= 0 && minute <= 0 && second <= 0) {
                qrcode_timeout()
                clearInterval(myTimer);

            }
            intDiff--;
        }, 1000);
    }



    function qrcode_timeout(){
        document.getElementById("orderbody").style.display = "none";
        document.getElementById("timeOut").style.display = "";
    }


    function getQueryString(name) {
        var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)", "i");
        var r = window.location.search.substr(1).match(reg);
        if (r != null)
            return decodeURI(r[2]);
        return null;
    }
    
    var really_price = '';
    var pay_type = '';
    var pay_id = '';
    var payUrl = '';
    $.ajaxSettings.async = false;
    $.post("../getOrder","orderId="+getQueryString("orderId"),function (data) {
        if (data.code==1){
            // console.log(data);
            really_price = data.data.reallyPrice;
            pay_type = data.data.payType;
            pay_id = data.data.payId;
            payUrl = data.data.payUrl;
            var time = new Date().getTime()-data.data.date*1000;
            time = time/1000;
            time = data.data.timeOut*60 - time;

            if (data.data.state == -1){
                time = 0;
            }
            timer(time);
            
            if (data.data.payType == 1) {
                data.data.payType1 = "微信";
            }else if (data.data.payType == 2) {
                data.data.payType1 = "支付宝";
            }else if (data.data.payType == 3) {
                data.data.payType1 = "QQ";
            }


            new Vue({
                el: '#body',
                data: data.data
            })

            check();
        }else{
            timer(0)
        }
    });
    $.ajaxSettings.async = true;

    function check() {
        $.post("../checkOrder","orderId="+getQueryString("orderId"),function (data) {
            //console.log(data);
            if (data.code == 1){
                if(userAgent.indexOf('alipay') != -1){
                    alert(data.msg);
                    window.open("","_self").close()
                }else{
                    window.location.href = data.data;
                }
            } else{
                if (data.data == "订单已过期") {
                    intDiff = 0;
                }else{
                    setTimeout("check()",1500);
                }
            }
        })
    }
    
    function isMobile() {
        var ua = navigator.userAgent;
        var ipad = ua.match(/(iPad).*OS\s([\d_]+)/),
            isIphone = !ipad && ua.match(/(iPhone\sOS)\s([\d_]+)/),
            isAndroid = ua.match(/(Android)\s+([\d.]+)/);
        return isIphone || isAndroid;
    }
    
    function toAccount(){
        money = really_price;
        if(money != ''){
            location.href = 'alipays://platformapi/startapp?appId=20000123&actionType=scan&biz_data={"s": "money","u": "'+userId+'","a": "'+money+'","m":"'+pay_id+'"}';
        }else{
            setTimeout('toAccount()',100);
        }
    }
    
    $('#show_qrcode').attr('src','/enQrcode?url='+encodeURIComponent(payUrl));
    
    var userId = '';
    $.ajaxSettings.async = false;
    $.post('/alipayInfo',{payId: pay_id},function(res){
        // console.log(res);
        if(res.code == 1){
            userId = res.alipayId;
        }
    })
    $.ajaxSettings.async = true;
    
    if(userId){
        /*if(userAgent.indexOf('alipay') != -1){
            setTimeout('toAccount()',100);
        }*/
        $('body').on('click','#alipay',function(){
            location.href = 'alipays://platformapi/startapp?appId=20000067&url=http://'+window.location.host+'/payPage/go_alipay.html?orderId='+getQueryString("orderId");
        });
        if(pay_type == 2){
            $('#orderbody .time-item strong').css('background','#00A0E8');
            $('.mod-ct .tip .ico-scan').css('background-image','url(ali-pay.png)');
            $('.mod-ct .detail .arrow .ico-arrow').css('background-image','url(ali-pay.png)');
            $('#show_qrcode').attr('src','/enQrcode?url=alipays://platformapi/startapp?appId=20000067&url=http://'+window.location.host+'/payPage/go_alipay.html?orderId='+getQueryString("orderId"));
            if(userAgent.indexOf('baidu') != -1){
                $('#alipay').hide();
            }
        }
    }else{
        $('body').on('click','#alipay',function(){
            location.href = 'alipayqr://platformapi/startapp?saId=10000007&clientVersion=3.7.0.0718&qrcode='+payUrl;
        });
    }

    $('body').on('click','#wxpay',function(){
        location.href = 'weixin://';
    })
    
    if (isMobile()) {
        $('.open_app').show();
    }
    
    $('#submitBd').on('click',function(){
        layer.prompt({title: '请输入你的邮箱，不填输入0', formType: 3}, function(email, index){
            layer.close(index);
            // console.log(email);
            layer.load(3);
            $.post('/submitBd',{payId: pay_id,email: email},function(res){
                if(res.code == 1){
                    layer.msg("提交补单成功，等待核实中",{time: 2000,icon: 1});
                }else{
                    layer.msg(res.msg,{time: 2000,icon: 2});
                }
                layer.closeAll('loading');
            })
        });
    })

    //监听加载状态改变
    document.onreadystatechange = completeLoading;
        //加载状态为complete时移除loading效果
        function completeLoading() {
            if (document.readyState == "complete") {
            $("#loading").animate({
            "opacity": "0"
            }, 500).hide(1000);
        }
    }
    
    if(userAgent.indexOf('alipay') == -1){
        text = '你好，你本次订单实际金额为'+really_price+'元，请按照金额进行付款。';
        $('body').append(`<audio autoplay>
          <source src="https://api.zzwll.cn/tts/api.php?text=`+text+`" type="audio/mpeg">
        您的浏览器不支持 audio 元素。
        </audio>`);
    }