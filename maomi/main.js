'ui';
importClass(android.view.View);
importClass(android.widget.Toast);
importClass(android.graphics.drawable.GradientDrawable);
ui.layout(
    <vertical padding="16">
        <text text="3D大赛工具" textSize="18sp" gravity="center"/>
        
        <horizontal>
            <text text="发送次数：" textSize="16sp" w="80"/>
            <input id="count" inputType="number" text="1" hint="请输入发送次数"/>
        </horizontal>
        
        <horizontal>
            <text text="间隔(ms)：" textSize="16sp" w="80"/>
            <input id="interval" inputType="number" text="500" hint="请输入间隔时间"/>
        </horizontal>
        
        <horizontal>
            <text text="商品ID：" textSize="16sp" w="80"/>
            <input id="goods_id" inputType="number" text="279964" hint="请输入goods_id"/>
        </horizontal>
        
        <horizontal gravity="center">
            <button id="start" text="开始发送" w="120"/>
            <button id="stop" text="停止发送" w="120" enabled="false"/>
            <button id="rizhi" text="打开日志" w="120" enabled="false"/>
        </horizontal>
                 

       
        <vertical h="500" scrollable="true">
            <text id="log" textSize="14sp" textColor="#666666" text="日志记录：\n"/>
        </vertical>
                 

    </vertical>
);

// 初始化全局变量
let isRunning = false;
let timer = null;
let sendCount = 0;


// 添加Unicode解码函数
function unicodeToText(str) {
    return str.replace(/\\u[\dA-F]{4}/gi, 
        function(match) {
            return String.fromCharCode(parseInt(match.replace(/\\u/g, ''), 16));
        });
}


 //更新日志显示
function updateLog(text) {
    ui.run(() => {
        ui.log.append("\n" + new Date().toLocaleString() + ": " + text);
    });
}

// 发送请求的核心函数
function sendRequest() {
    try {
        let url = "https://ds.3ddl.net//index.php?ctl=Goods_Goods&met=admireGoods&typ=json";
        let body = "k=AHICJwJoVSVRAwBuUDUCbgRlBDQ1JMwdiVWRRMVEmU2FXOAk%2BD2lSPlYEB2laP1o2BWYGJ1QkVmIDa1dzU11RaQBjAmkCPQ%3D%3D&u=279964&goods_id=" + ui.goods_id.text();
        
        let ret = http.request(url, {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
                "User-Agent": "Mozilla/5.0 (Linux; Android 6.0.1; OPPO R9s Plus Build/MMB29M; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/55.0.2883.91 Mobile Safari/537.36"
            },
            body: body
        });
      //  console.log(ret.body.string());
        sendCount++;
        let result = ret.body.string();
                // 解码Unicode响应
        let decodedResult = unicodeToText(result);
        updateLog("第" + sendCount + "次发送成功：" );
        console.info("第" + sendCount + "次发送成功：" + decodedResult);
    } catch (e) {
        updateLog("发送失败：" + e.message);
    }
}

ui.rizhi.click(function(){
app.startActivity('console');
  });
// 开始按钮点击事件
ui.start.click(() => {
    if (!ui.goods_id.text()) {
        toast("请先输入商品ID");
        return;
    }
    isRunning = true;
    sendCount = 0;
    ui.start.enabled = false;  // 修改这里：使用属性赋值而非函数调用
    ui.stop.enabled = true;    // 修改这里：使用属性赋值而非函数调用
    
    let totalCount = parseInt(ui.count.text()) || 1;
    let intervalTime = parseInt(ui.interval.text()) || 1;
    
    threads.start(function() {
        for (let i = 0; i < totalCount && isRunning; i++) {
            sendRequest();
            if (i < totalCount - 1) {
                sleep(intervalTime);
            }
        }
        ui.run(() => {
            ui.start.enabled = true;    // 修改这里
            ui.stop.enabled = false;   // 修改这里
            isRunning = false;
        });
    });
});

// 停止按钮点击事件
ui.stop.click(() => {
    isRunning = false;
    ui.start.enabled = true;    // 修改这里
    ui.stop.enabled = false;    // 修改这里
    updateLog("用户手动停止发送");
});

// 初始化提示
updateLog("等待开始发送...");