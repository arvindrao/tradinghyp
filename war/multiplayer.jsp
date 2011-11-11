<html>
<head>
<meta name="description" content="A multiplayer trading simulation. Trade against your friends and other players from around the world!" />
<meta name="keywords" content="trading,stock trading,trading sim,trading simulation,multiplayer game" />
<title>TRADING HYP - Multiplayer Mode</title>
<link rel="icon" type="image/png" href="images/favicon.png" />
<link rel="shortcut icon" type="image/png" href="images/favicon.png" />
<link rel="stylesheet" type="text/css" href="styles/multiplayerstyle.css" />

<script type="text/javascript" src="/_ah/channel/jsapi"></script>
<script type="text/javascript">
var userId="";
var traderId="-";
var channel;
var handler;
var socket;

var dayVol=0;
var dayVWAP=0;
var dayTradeValue=0;
var tradeVol;
var tradeValue;
var lastTradeTime;
var lastPrice;
var myPrice;
var lastVol;
var openPrice=0;
var closePrice;
var highPrice;
var lowPrice;
var changeFromClose;
var changeFromClosePercent;
var row;
var cell;
var cash=0;
var mktValue=0;
var cost=0;
var position=0;
var unrealizedGain=0;
var unrealizedGainPercent=0;
var realizedGain=0;
var realizedGainPercent=0;
var longPosition=0;
var longCash=0;
var shortPosition=0;
var shortCash=0;
var longAvgPx=0;
var shortAvgPx=0;
var orderPrice=0;
var activeBuyOrders=new Array();
var activeSellOrders=new Array();

var MIN_USR_QTY=1;
var MAX_USR_QTY=10;
var MAX_USR_POS=200;
var MAX_HIGH_SCORES=11;
var MAX_ACTIVE_ORDERS=2;
var LOT_SIZE=100;
var TICK_SIZE=0.01;

var changeSign="+";
var unrealizedGainSign="+";
var realizedGainSign="+";

function updateTradeStats(){
	if (longPosition>0){
		//player is long
		mktValue=longPosition*lastPrice;
		unrealizedGain=mktValue-(longPosition*longAvgPx);
		unrealizedGainPercent=(unrealizedGain/(longPosition*longAvgPx))*100;
	}
	else if (shortPosition>0){
		//player is short
		mktValue=shortPosition*lastPrice;
		unrealizedGain=(shortPosition*shortAvgPx)-mktValue;
		unrealizedGainPercent=(unrealizedGain/(shortPosition*shortAvgPx))*100;
	}
	else{
		//player is flat
		mktValue=0;
		unrealizedGain=0;
		unrealizedGainPercent=0;
	}
	if (longPosition-shortPosition>0){
		myPos="Long";
	}
	else if (longPosition-shortPosition<0){
		myPos="Short";
	}
	else{
		myPos="";	
	}
	document.getElementById("myPosition").innerHTML=myPos+" "+Math.abs(longPosition-shortPosition);
	document.getElementById("myMktValue").innerHTML=mktValue.toFixed(2);
	if(unrealizedGain>=0){
		unrealizedGainSign="+";
		document.getElementById("myUnrealizedGain").style.color='#70D400';
		document.getElementById("myUnrealizedGainPercent").style.color='#70D400';
	}
	else{
		unrealizedGainSign="";
		document.getElementById("myUnrealizedGain").style.color='#FF5738';
		document.getElementById("myUnrealizedGainPercent").style.color='#FF5738';
	}

	document.getElementById("myUnrealizedGain").innerHTML=unrealizedGainSign+unrealizedGain.toFixed(2);
	document.getElementById("myUnrealizedGainPercent").innerHTML="("+unrealizedGainSign+unrealizedGainPercent.toFixed(2)+"%)";
}

function updatePositions(myScore){
	realizedGain=myScore.realizedGain;
	longPosition=myScore.longPosition;
	longAvgPx=myScore.longAvgPrice;
	shortPosition=myScore.shortPosition;
	shortAvgPx=myScore.shortAvgPrice;
	
	if (realizedGain>=0){
		document.getElementById("myRealizedGain").style.color='#70D400';
		document.getElementById("myRealizedGain").innerHTML="+"+realizedGain.toFixed(2);
	}
	else{
		document.getElementById("myRealizedGain").style.color='#FF5738';
		document.getElementById("myRealizedGain").innerHTML=realizedGain.toFixed(2);
	}
	
	if (longPosition-shortPosition>0){
		document.getElementById("myAvgPrice").innerHTML=longAvgPx.toFixed(2);
	}
	else if (longPosition-shortPosition<0){
		document.getElementById("myAvgPrice").innerHTML=shortAvgPx.toFixed(2);
	}
	else{
		document.getElementById("myAvgPrice").innerHTML="0.00";
	}
}

chOpen=function(){
	var mktDataRefreshReq = new XMLHttpRequest();
	mktDataRefreshReq.onreadystatechange = function(){
	
	};
	
	mktDataRefreshReq.open("POST","/refreshMarketData.do",true);

	mktDataRefreshReq.setRequestHeader("Content-Type","application/x-www-form-urlencoded");
	mktDataRefreshReq.send("symbol=HYP");
	
}

chClose=function(){
	document.getElementById("alerts").innerHTML="Logged off from trading system.";
}

chError=function(error){
	document.getElementById("alerts").innerHTML="Disconnected from the system:"+error.code+" "+error.description;
}

chMessage=function(msg){
	var msgData =JSON.parse(msg.data);
	var a;
	if (msgData.type=="MKTDATA"){
		for (a=0;a<msgData.bid.length;a++){
			document.getElementById("bidPrice"+a).innerHTML=msgData.bid[a].price.toFixed(2);
			document.getElementById("bidVol"+a).innerHTML=msgData.bid[a].vol;
			document.getElementById("askPrice"+a).innerHTML=msgData.ask[a].price.toFixed(2);
			document.getElementById("askVol"+a).innerHTML=msgData.ask[a].vol;
		}
		
		for (a=0;a<msgData.trade.length;a++){
			document.getElementById("trades").deleteRow(1);
			row=document.getElementById("trades").insertRow(-1);
			row.insertCell(0).innerHTML=msgData.trade[a].time;
			row.insertCell(1).innerHTML=msgData.trade[a].qty;
			row.insertCell(2).innerHTML=msgData.trade[a].price.toFixed(2);
			row.insertCell(3).innerHTML=msgData.trade[a].buyerId;
			row.insertCell(4).innerHTML=msgData.trade[a].sellerId;
		}
		
		closePrice=msgData.closePrice;
		openPrice=msgData.openPrice;
		highPrice=msgData.highPrice;
		lowPrice=msgData.lowPrice;
		lastPrice=msgData.lastPrice;
		lastVol=msgData.lastVol;
		changeFromClose=msgData.changeFromClose;
		changeFromClosePercent=msgData.changeFromClosePercent;
		dayVol=msgData.dayVol;
		dayVWAP=msgData.dayVWAP;

		document.getElementById("closePrice").innerHTML=closePrice.toFixed(2);
		document.getElementById("openPrice").innerHTML=openPrice.toFixed(2);
		document.getElementById("highPrice").innerHTML=highPrice.toFixed(2);
		document.getElementById("lowPrice").innerHTML=lowPrice.toFixed(2);
		if (changeFromClose>=0){
			changeSign="+";
			document.getElementById("changeFromClose").style.color='#70D400';
		}
		else{
			changeSign="";
			document.getElementById("changeFromClose").style.color='#FF5738';
		}
	
		document.getElementById("changeFromClose").innerHTML=changeSign+changeFromClose.toFixed(2)+" ("+changeSign+changeFromClosePercent.toFixed(2)+"%)";
		document.getElementById("lastTradePrice").innerHTML=lastPrice.toFixed(2);
		document.getElementById("lastTradeVol").innerHTML=lastVol;
		document.getElementById("dayVWAP").innerHTML=dayVWAP.toFixed(2);
		document.getElementById("dayVol").innerHTML=dayVol;
		
		updateTradeStats();
		
		var ind=1;
		for (a=0;a<msgData.highScore.length;a++){
			document.getElementById("highScores").deleteRow(ind);
			row=document.getElementById("highScores").insertRow(ind);
			row.insertCell(0).innerHTML=msgData.highScore[a].score.toFixed(2);
			row.insertCell(1).innerHTML=msgData.highScore[a].userId+" ("+msgData.highScore[a].traderId+")";
			ind++;
		}
		if (msgData.highScore.length>0){
			for (a=ind;a<=MAX_HIGH_SCORES;a++){
				//blank out remaining high scores
				document.getElementById("highScores").deleteRow(ind);
				row=document.getElementById("highScores").insertRow(ind);
				row.insertCell(0).innerHTML="-";
			}
		}
	}
	else if (msgData.type=="ORDER"){
		for (a=0;a<msgData.order.length;a++){
			var found=0;
			var b=0;
	
			if (msgData.order[a].side=="B"){
				//check if its an update to an existing active B order
				for (b=0;b<activeBuyOrders.length;b++){
					if (msgData.order[a].orderId==activeBuyOrders[b].orderId){
						if (msgData.order[a].status=="Working"){
							//order is still working, update it
							activeBuyOrders[b]=msgData.order[a];
						}
						else{
							//order is completed, remove it from working list
							activeBuyOrders.splice(b,1);
							document.getElementById("myCompletedOrders").deleteRow(1);
							row=document.getElementById("myCompletedOrders").insertRow(-1);
							row.insertCell(0).innerHTML=msgData.order[a].orderId;
							row.insertCell(1).innerHTML=msgData.order[a].side;
							row.insertCell(2).innerHTML=msgData.order[a].orderQty;
							row.insertCell(3).innerHTML=msgData.order[a].limitPrice.toFixed(2);
							row.insertCell(4).innerHTML=msgData.order[a].execQty;
							row.insertCell(5).innerHTML=msgData.order[a].avgPrice.toFixed(2);
							row.insertCell(6).innerHTML=msgData.order[a].status;
						}
						found=1;
						break;
					}
				}
				if (found==0){
					//not an update
					if (msgData.order[a].status=="Working"){
						activeBuyOrders.push(msgData.order[a]);
					}
					else{
						document.getElementById("myCompletedOrders").deleteRow(1);
						row=document.getElementById("myCompletedOrders").insertRow(-1);
						row.insertCell(0).innerHTML=msgData.order[a].orderId;
						row.insertCell(1).innerHTML=msgData.order[a].side;
						row.insertCell(2).innerHTML=msgData.order[a].orderQty;
						row.insertCell(3).innerHTML=msgData.order[a].limitPrice.toFixed(2);
						row.insertCell(4).innerHTML=msgData.order[a].execQty;
						row.insertCell(5).innerHTML=msgData.order[a].avgPrice.toFixed(2);
						row.insertCell(6).innerHTML=msgData.order[a].status;
					}
				}
			}
			else{
				//check if its an update to an existing active S order
				for (b=0;b<activeSellOrders.length;b++){
					if (msgData.order[a].orderId==activeSellOrders[b].orderId){
						if (msgData.order[a].status=="Working"){
							//order is still working, update it
							activeSellOrders[b]=msgData.order[a];
						}
						else{
							//order is completed, remove it from working list
							activeSellOrders.splice(b,1);
							document.getElementById("myCompletedOrders").deleteRow(1);
							row=document.getElementById("myCompletedOrders").insertRow(-1);
							row.insertCell(0).innerHTML=msgData.order[a].orderId;
							row.insertCell(1).innerHTML=msgData.order[a].side;
							row.insertCell(2).innerHTML=msgData.order[a].orderQty;
							row.insertCell(3).innerHTML=msgData.order[a].limitPrice.toFixed(2);
							row.insertCell(4).innerHTML=msgData.order[a].execQty;
							row.insertCell(5).innerHTML=msgData.order[a].avgPrice.toFixed(2);
							row.insertCell(6).innerHTML=msgData.order[a].status;
						}
						found=1;
						break;
					}
				}
				if (found==0){
					//not an update
					if (msgData.order[a].status=="Working"){
						activeSellOrders.push(msgData.order[a]);
					}
					else{
						document.getElementById("myCompletedOrders").deleteRow(1);
						row=document.getElementById("myCompletedOrders").insertRow(-1);
						row.insertCell(0).innerHTML=msgData.order[a].orderId;
						row.insertCell(1).innerHTML=msgData.order[a].side;
						row.insertCell(2).innerHTML=msgData.order[a].orderQty;
						row.insertCell(3).innerHTML=msgData.order[a].limitPrice.toFixed(2);
						row.insertCell(4).innerHTML=msgData.order[a].execQty;
						row.insertCell(5).innerHTML=msgData.order[a].avgPrice.toFixed(2);
						row.insertCell(6).innerHTML=msgData.order[a].status;
					}
				}
	
			}

		}				

		//update active orders
		var ind=1;
		for (b=0;b<activeBuyOrders.length;b++){
			document.getElementById("myWorkingOrders").deleteRow(ind);
			row=document.getElementById("myWorkingOrders").insertRow(ind);
			row.insertCell(0).innerHTML=activeBuyOrders[b].orderId;
			row.insertCell(1).innerHTML=activeBuyOrders[b].side;
			row.insertCell(2).innerHTML=activeBuyOrders[b].orderQty;
			row.insertCell(3).innerHTML=activeBuyOrders[b].limitPrice.toFixed(2);
			row.insertCell(4).innerHTML=activeBuyOrders[b].execQty;
			row.insertCell(5).innerHTML=activeBuyOrders[b].avgPrice.toFixed(2);
			row.insertCell(6).innerHTML=activeBuyOrders[b].status;
			newrow=row.insertCell(7);
			cxlButton=document.createElement("input");
			cxlButton.setAttribute("type","button");
			cxlButton.setAttribute("onclick","cancelOrder('"+activeBuyOrders[b].orderId+"')");
			cxlButton.setAttribute("value","X");
			cxlButton.setAttribute("class","cxlButton");
			cxlButton.setAttribute("style.display","none");
			cxlButton.setAttribute("style.display","block");
			newrow.appendChild(cxlButton);
			ind++;
			
		} 
		for (b=0;b<activeSellOrders.length;b++){
			document.getElementById("myWorkingOrders").deleteRow(ind);
			row=document.getElementById("myWorkingOrders").insertRow(ind);
			row.insertCell(0).innerHTML=activeSellOrders[b].orderId;
			row.insertCell(1).innerHTML=activeSellOrders[b].side;
			row.insertCell(2).innerHTML=activeSellOrders[b].orderQty;
			row.insertCell(3).innerHTML=activeSellOrders[b].limitPrice.toFixed(2);
			row.insertCell(4).innerHTML=activeSellOrders[b].execQty;
			row.insertCell(5).innerHTML=activeSellOrders[b].avgPrice.toFixed(2);
			row.insertCell(6).innerHTML=activeSellOrders[b].status;
			newrow=row.insertCell(7);
			cxlButton=document.createElement("input");
			cxlButton.setAttribute("type","button");
			cxlButton.setAttribute("onclick","cancelOrder('"+activeSellOrders[b].orderId+"')");
			cxlButton.setAttribute("value","X");
			cxlButton.setAttribute("class","cxlButton");
			cxlButton.setAttribute("style.display","none");
			cxlButton.setAttribute("style.display","block");
			newrow.appendChild(cxlButton);
			ind++;	
		} 

		for (b=ind;b<=MAX_ACTIVE_ORDERS*2;b++){
			//blank out remaining rows
			document.getElementById("myWorkingOrders").deleteRow(b);
			row=document.getElementById("myWorkingOrders").insertRow(b);
			row.insertCell(0).innerHTML="-";
			row.insertCell(1).innerHTML="";
			row.insertCell(2).innerHTML="";
			row.insertCell(3).innerHTML="";
			row.insertCell(4).innerHTML="";
			row.insertCell(5).innerHTML="";
			row.insertCell(6).innerHTML="";
		}
		
		if (typeof msgData.score != "undefined"){
			updatePositions(msgData.score);
		}
	}
	
}


function initializeOrderBook(){

	document.getElementById("closePrice").innerHTML="0.00";
	document.getElementById("tickSize").innerHTML=TICK_SIZE.toFixed(2);
	document.getElementById("lotSize").innerHTML=LOT_SIZE+" shares";
	document.getElementById("changeFromClose").innerHTML="+0.00 (+0.00%)";
	document.getElementById("highPrice").innerHTML="0.00";
	document.getElementById("lowPrice").innerHTML="0.00";
	document.getElementById("openPrice").innerHTML="0.00";
	document.getElementById("lastTradePrice").innerHTML="0.00";
	document.getElementById("lastTradeVol").innerHTML="0";
	document.getElementById("dayVWAP").innerHTML="0.00";
	document.getElementById("dayVol").innerHTML="0";

}

function initializePlayer(){
	cash=0;
	mktValue=0;
	cost=0;
	position=0;
	unrealizedGain=0;
	unrealizedGainPercent=0;
	realizedGain=0;
	realizedGainPercent=0;
	longPosition=0;
	shortPosition=0;
	longAvgPx=0;
	shortAvgPx=0;
	changeSign="+";
	unrealizedGainSign="+";
	realizedGainSign="+";
	
	document.getElementById("myMktValue").innerHTML=Number(0).toFixed(2);
	document.getElementById("myUnrealizedGain").style.color='#70D400';
	document.getElementById("myUnrealizedGain").innerHTML="+0.00";
	document.getElementById("myUnrealizedGainPercent").style.color='#70D400';
	document.getElementById("myUnrealizedGainPercent").innerHTML="(+0.00%)";
	document.getElementById("myPosition").innerHTML=0;
	document.getElementById("myRealizedGain").style.color='#70D400';
	document.getElementById("myRealizedGain").innerHTML="+0.00";
	document.getElementById("myAvgPrice").innerHTML="0.00";
	document.getElementById("mktCheckBox").checked=false;
	document.getElementById("lmtCheckBox").checked=true;
	
	var nickname=document.getElementById("myNickname").value;
	userId=nickname.replace(/\W/g,"");
	if (userId==""){
		userId="Anonymous";
	}
}

function startNewGame(){
	document.getElementById("gameTerminal").style.display="block";
	document.getElementById("bButton").disabled="";
	document.getElementById("sButton").disabled="";

	document.getElementById("notLoggedIn").style.display="none";
	
	initializePlayer();
	initializeOrderBook();

	var channelReq = new XMLHttpRequest();
	channelReq.onreadystatechange = function(){
		 if (channelReq.readyState==4 && channelReq.status==200) {
			 	var chResp=JSON.parse(channelReq.responseText);
			 	if (chResp.connected=="Y"){
					channel = new goog.appengine.Channel(chResp.token);
					handler = {
					          'onopen': chOpen,
					          'onmessage': chMessage,
					          'onerror': chError,
					          'onclose': chClose
					        };
					socket = channel.open(handler);
					traderId=chResp.traderId;
					document.getElementById("alerts").innerHTML="Your Trader ID is "+traderId+". There are "+chResp.activeTraderCount+" players connected now, including a market maker and a price taker bot!";
					document.getElementById("myTraderId").innerHTML="Trader ID:"+traderId+" ("+userId+")";
					document.getElementById("loggedIn").style.display="inline";
					document.getElementById("myUserId").innerHTML=userId;
			 	}
			 	else{
			 		document.getElementById("alerts").innerHTML="Cannot connect to game session. Refresh the page and try again in a few minutes!";
			 	}
		 }
	
	};
	
	channelReq.open("POST","/initConnection.do",true);

	channelReq.setRequestHeader("Content-Type","application/x-www-form-urlencoded");
	channelReq.send("userId="+userId);
	
}

function quitGame(){
	if (confirm("Are you sure you want to quit the game?")==false){
		return;
	}
	socket.close();
	document.getElementById("loggedIn").style.display="none";
	document.getElementById("notLoggedIn").style.display="block";
	document.getElementById("gameTerminal").style.display="none";
	location.reload(true);
	
}

function newOrder(side){
	var req;
	var ptype=document.getElementById("mktCheckBox").checked?"MKT":"LMT";

	if (isNaN(document.getElementById("myVol").value) || document.getElementById("myVol").value==""){
		document.getElementById("alerts").innerHTML="Incorrect order size..use multiples of the round lot size, "+LOT_SIZE;
		tradeVol=0;
	}
	else if (ptype=="LMT" && (isNaN(document.getElementById("myPrice").value) || document.getElementById("myPrice").value=="")){
		document.getElementById("alerts").innerHTML="Bad price..use multiples of "+TICK_SIZE.toFixed(2);
		tradeVol=0;
	}
	else{
		//valid number
		tradeVol = Number(document.getElementById("myVol").value);
		orderPrice = ptype=="MKT"?0:Number(document.getElementById("myPrice").value);

		if (tradeVol<LOT_SIZE || tradeVol%LOT_SIZE!=0){
			document.getElementById("alerts").innerHTML="Incorrect order size..use multiples of the round lot size, "+LOT_SIZE;
			tradeVol=0;
		}
		else if (tradeVol>MAX_USR_QTY*LOT_SIZE ){
			document.getElementById("alerts").innerHTML="Your order size limit is "+MAX_USR_QTY*LOT_SIZE+" shares";
			tradeVol=0;
		}
		else if (ptype=="LMT" && ((orderPrice*100)%(TICK_SIZE*100)!=0 || orderPrice<=0)){
			document.getElementById("alerts").innerHTML="Bad price..use multiples of "+TICK_SIZE.toFixed(2);
			tradeVol=0;
		}
		else if (ptype=="LMT" && ((orderPrice-lastPrice)/lastPrice>0.05 || (orderPrice-lastPrice)/lastPrice<-0.05)){
			document.getElementById("alerts").innerHTML="Bad price..more than 5% away from "+lastPrice.toFixed(2);
			tradeVol=0;
		}
		else{
			tradeValue=0;
			lastTradeTime=new Date();
			
			var outstandingLongs=0;
			var outstandingShorts=0;
			var co;
			for (co=0;co<activeBuyOrders.length;co++){
				outstandingLongs+=activeBuyOrders[co].orderQty-activeBuyOrders[co].execQty;
			}
			for (co=0;co<activeSellOrders.length;co++){
				outstandingShorts+=activeSellOrders[co].orderQty-activeSellOrders[co].execQty;
			}

			if (side=="B" && (longPosition+outstandingLongs-shortPosition-outstandingShorts+tradeVol>MAX_USR_POS*LOT_SIZE)){
				document.getElementById("alerts").innerHTML="Your long position limit is "+MAX_USR_POS*LOT_SIZE+"..close out your position or cancel some buy orders";
				tradeVol=0;
			}
			else if (side=="B" && (activeBuyOrders.length+1>MAX_ACTIVE_ORDERS)){
				document.getElementById("alerts").innerHTML="You already have "+MAX_ACTIVE_ORDERS+" buys on the book..cancel one of them";
				tradeVol=0;
			}
			else if (side=="S" && (longPosition+outstandingLongs-shortPosition-outstandingShorts-tradeVol< -1*MAX_USR_POS*LOT_SIZE)){
				document.getElementById("alerts").innerHTML="Your short position limit is "+MAX_USR_POS*LOT_SIZE+"..close out your position or cancel some sell orders";
				tradeVol=0;
			}
			else if (side=="S" && (activeSellOrders.length+1>MAX_ACTIVE_ORDERS)){
				document.getElementById("alerts").innerHTML="You already have "+MAX_ACTIVE_ORDERS+" sells on the book..cancel one of them";
				tradeVol=0;
			}
			else{
				document.getElementById("alerts").innerHTML="";

				req = new XMLHttpRequest();
				
				req.onreadystatechange = function(){
					if (req.readyState==4 && req.status==200){
						document.getElementById("alerts").innerHTML=req.responseText;
					 }
				}
				
				req.open("POST","/invokeOrder.do",true);

				req.setRequestHeader("Content-Type","application/x-www-form-urlencoded");
				req.send("symbol=HYP&side="+side+"&qty="+tradeVol+"&price="+orderPrice+"&orderType=NEW");
				
				document.getElementById("myVol").value="";
				document.getElementById("myPrice").value="";
			}

		}
	}
}

function cancelOrder(orderId){
	if (confirm("Are you sure you want to cancel order #"+orderId+" ?")==false){
		return;
	}
	document.getElementById("alerts").innerHTML="";

	req = new XMLHttpRequest();
	
	req.onreadystatechange = function(){
		if (req.readyState==4 && req.status==200){
			document.getElementById("alerts").innerHTML=req.responseText;
		 }
	}
	
	req.open("POST","/invokeOrder.do",true);

	req.setRequestHeader("Content-Type","application/x-www-form-urlencoded");
	req.send("symbol=HYP&orderType=CXL&orderId="+orderId);
	
}

function priceType(ptype){
	if (ptype=="MKT"){
		document.getElementById("lmtCheckBox").checked=false;
		document.getElementById("myPrice").value="";
		document.getElementById("myPrice").disabled=true;
	}
	else if (ptype=="LMT"){
		document.getElementById("mktCheckBox").checked=false;
		document.getElementById("myPrice").disabled=false;
	}
} 

</script>


<script type="text/javascript">

  var _gaq = _gaq || [];
  _gaq.push(['_setAccount', 'UA-25201537-1']);
  _gaq.push(['_trackPageview']);

  (function() {
    var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
    ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
    var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
  })();

</script>
</head>

<body>
<div id="gameTerminal">
<table class="mainScreen" id="mainPage" border=0 cellpadding=0 cellspacing=1>
<tr>

<td><div id="companyName">Hypothetical Systems Corp (Ticker:HYP)</div></td>
<td colspan="2"><div id="myTraderId">-</div></td>
</tr>

<tr>
<td><div class="headerSections">MARKET DATA</div></td>
<td><div class="headerSections">MY SCORE</div></td>
<td><div class="headerSections">HIGH SCORES</div></td>
</tr>

<tr>
<td>

<table id="quoteSection" class="marketDataScreen">
<tr>
<td class="mdHeader">Last</td>
<td class="mdHeader">Last Volume</td>
<td  class="mdHeader" colspan="2">Change</td>
</tr>
<tr>
<td><div id="lastTradePrice" class="tradeNumbers"></div></td>
<td><div id="lastTradeVol" class="tradeNumbers"/></div></td>
<td colspan="2"><div id="changeFromClose" class="tradeNumbers"></div></td>
</tr>
<tr>
<td class="mdHeader">Open</td>
<td class="mdHeader">High</td>
<td class="mdHeader">Low</td>
<td class="mdHeader">Close</td>
</tr>
<tr>
<td><div  id="openPrice" class="tradeNumbers"></div></td>
<td><div  id="highPrice" class="tradeNumbers"></div></td>
<td><div  id="lowPrice" class="tradeNumbers"></div></td>
<td><div  id="closePrice" class="tradeNumbers"></div></td>
</tr>
<tr>
<td class="mdHeader">VWAP</td>
<td class="mdHeader">Day Volume</td>
<td class="mdHeader">Tick Size</td>
<td class="mdHeader">Round Lot</td>
</tr>
<tr>
<td><div  id="dayVWAP" class="tradeNumbers"></div></td>
<td><div  id="dayVol" class="tradeNumbers"></div></td>
<td><div  id="tickSize" class="tradeNumbers"></div></td>
<td><div  id="lotSize" class="tradeNumbers"></div></td>
</tr>
</table>
</td>

<td rowspan="2">
<table id="playerPosition" class="playerDataScreen">
<tr>
<td colspan="2" class="mdHeader">Realized P&L</td>
</tr>
<tr>
<td colspan="2" ><div  id="myRealizedGain" class="posNumbers"></div></td>
</tr>
<tr>
<td colspan="2" class="mdHeader">Unrealized P&L</td>
</tr>
<tr>
<td colspan="2" ><div  id="myUnrealizedGain" class="posNumbers"></div></td>
</tr>

<tr>
<td colspan="2" ><div  id="myUnrealizedGainPercent" class="posNumbers"></div></td>
</tr>

<tr>
<td class="mdHeader">Net Position</td>
<td class="mdHeader">Average Price</td>
</tr>
<tr>
<td><div  id="myPosition" class="posNumbers"></div></td>
<td><div  id="myAvgPrice" class="posNumbers"></div></td>
</tr>
<tr>
<td colspan="2" class="mdHeader">Market value</td>
</tr>

<tr>
<td colspan="2"><div  id="myMktValue" class="posNumbers"></div></td>
</tr>

</table>

</td>

<td rowspan="2">
<table id="highScores" class="playerDataScreen">
<tr><td class="mdHeader">Realized P&L</td><td class="mdHeader">Player (ID)</td></tr>
<tr><td><div class="scoreNumbers">-</div></td><td><div class="scoreNumbers" ></div></td></tr>
<tr><td><div class="scoreNumbers">-</div></td><td><div class="scoreNumbers" ></div></td></tr>
<tr><td><div class="scoreNumbers">-</div></td><td><div class="scoreNumbers" ></div></td></tr>
<tr><td><div class="scoreNumbers">-</div></td><td><div class="scoreNumbers" ></div></td></tr>
<tr><td><div class="scoreNumbers">-</div></td><td><div class="scoreNumbers" ></div></td></tr>
<tr><td><div class="scoreNumbers">-</div></td><td><div class="scoreNumbers" ></div></td></tr>
<tr><td><div class="scoreNumbers">-</div></td><td><div class="scoreNumbers" ></div></td></tr>
<tr><td><div class="scoreNumbers">-</div></td><td><div class="scoreNumbers" ></div></td></tr>
<tr><td><div class="scoreNumbers">-</div></td><td><div class="scoreNumbers" ></div></td></tr>
<tr><td><div class="scoreNumbers">-</div></td><td><div class="scoreNumbers" ></div></td></tr>
<tr><td><div class="scoreNumbers">-</div></td><td><div class="scoreNumbers" ></div></td></tr>
</table>
</td>

</tr>
<tr>
<td padding-top="0">
<table id="marketDepth" class="marketDataScreen">
<tr><td class="mdHeader">Size</td><td class="mdHeader">Bid</td><td class="mdHeader">Ask</td><td class="mdHeader">Size</td></tr>
<tr><td><div class="bids"  id="bidVol0" >0</div></td><td><div class="bids"  id="bidPrice0" >0</div></td>
<td><div class="asks"  id="askPrice0" >0</div></td><td><div  class="asks" id="askVol0" >0</div></td></tr>
<tr><td><div class="bids"  id="bidVol1" >0</div></td><td><div  class="bids" id="bidPrice1" >0</div></td>
<td><div class="asks"  id="askPrice1" >0</div></td><td><div  class="asks" id="askVol1" >0</div></td></tr>
<tr><td><div class="bids"  id="bidVol2" >0</div></td><td><div class="bids"  id="bidPrice2" >0</div></td>
<td><div class="asks"  id="askPrice2" >0</div></td><td><div  class="asks" id="askVol2" >0</div></td></tr>
<tr><td><div class="bids"  id="bidVol3" >0</div></td><td><div class="bids"  id="bidPrice3" >0</div></td>
<td><div class="asks"  id="askPrice3" >0</div></td><td><div  class="asks" id="askVol3" >0</div></td></tr>
<tr><td><div class="bids"  id="bidVol4" >0</div></td><td><div class="bids"  id="bidPrice4" >0</div></td>
<td><div class="asks"  id="askPrice4" >0</div></td><td><div  class="asks" id="askVol4" >0</div></td></tr>
</table>

</td>


</tr>


<tr>
<td><div class="headerSections">MY WORKING ORDERS</div></td>
<td><div class="headerSections">MY COMPLETED ORDERS</div></td>
<td><div class="headerSections"">TRADE HISTORY</div></td>

</tr>

<tr>
<td>
<table id="myWorkingOrders" class="playerDataScreen">
<tr>
<td class="mdHeader">Order#</td><td class="mdHeader">Side</td><td class="mdHeader">Order Size</td><td class="mdHeader">Limit</td>
<td class="mdHeader">Executed</td><td class="mdHeader">Avg Price</td><td class="mdHeader">Status</td><td class="mdHeader"></td>
</tr>
<tr><td>-</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
<tr><td>-</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
<tr><td>-</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
<tr><td>-</td><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
</table>

</td>

<td rowspan="4">
<table id="myCompletedOrders" class="playerDataScreen">
<td class="mdHeader">Order#</td><td class="mdHeader">Side</td><td class="mdHeader">Order Size</td><td class="mdHeader">Limit</td>
<td class="mdHeader">Executed</td><td class="mdHeader">Avg Price</td><td class="mdHeader">Status</td><td class="mdHeader"></td>
<tr><td>-</td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
<tr><td>-</td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
<tr><td>-</td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
<tr><td>-</td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
<tr><td>-</td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
<tr><td>-</td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
<tr><td>-</td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
<tr><td>-</td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
<tr><td>-</td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
<tr><td>-</td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
<tr><td>-</td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
<tr><td>-</td><td></td><td></td><td></td><td></td><td></td><td></td></tr>
</table>
</td>

<td rowspan="4">
<table id="trades" class="marketDataScreen">
<tr><td class="mdHeader">Time(GMT)</td><td class="mdHeader">Size</td>
<td class="mdHeader">Price</td><td class="mdHeader">Buyer ID</td><td class="mdHeader">Seller ID</td></tr>
<tr><td>-</td><td></td><td></td><td></td><td></td></tr>
<tr><td>-</td><td></td><td></td><td></td><td></td></tr>
<tr><td>-</td><td></td><td></td><td></td><td></td></tr>
<tr><td>-</td><td></td><td></td><td></td><td></td></tr>
<tr><td>-</td><td></td><td></td><td></td><td></td></tr>
<tr><td>-</td><td></td><td></td><td></td><td></td></tr>
<tr><td>-</td><td></td><td></td><td></td><td></td></tr>
<tr><td>-</td><td></td><td></td><td></td><td></td></tr>
<tr><td>-</td><td></td><td></td><td></td><td></td></tr>
<tr><td>-</td><td></td><td></td><td></td><td></td></tr>
<tr><td>-</td><td></td><td></td><td></td><td></td></tr>
<tr><td>-</td><td></td><td></td><td></td><td></td></tr>
</table>
</td>

</tr>

<tr>
<td>
<table id="orderEntry" class="playerDataScreen">
<tr>
<td class="mdHeader">Quantity</td><td><input type=text id="myVol" class="inputText"/></td>
<td colspan="2"><input type=button onclick="newOrder('B')" value="BUY" class="buyButton" id="bButton" /></td>
<td colspan="2"><input type=button onclick="newOrder('S')" value="SELL" class="sellButton" id="sButton"/></td>
</tr>
<tr>
<td class="mdHeader">Price</td><td><input type=text id="myPrice" class="inputText"/></td>
<td class="mdHeader" align="center">Limit</td>
<td><input type="checkbox" onclick="priceType('LMT')" value="LMT" class="priceCheckBox" id="lmtCheckBox"/></td>
<td class="mdHeader" align="center">Market</td>
<td><input type="checkbox" onclick="priceType('MKT')" value="MKT" class="priceCheckBox" id="mktCheckBox"/></td>
</tr>
</table>
</td>
</tr>

<tr>
<td><div class="headerSections">MY MESSAGES</div></td>
</tr>

<tr>
<td>
<div id="alerts"></div>
</td>
</tr>
<tr>
<td colspan="3">
<div id="infoMsg">
<br>
This is the TRADING HYP game screen. Scroll down to see the game rules and other information.
</div>
</td>
</tr>
</table>
<br>
<br>
</div>

<div id="helpPage" class="helpScreen">

<div id="helpHeader">TRADING HYP - Multiplayer Mode</div>
<div id="helpAuthor">by Arvind Rao</div>
<hr>
<div id="helpBody">
<p>
You trade the stock of Hypothetical Systems Corp (HYP) in this multiplayer game. Trade against your friends and other players from around the world!
<br><br>
<b>No sign-up needed! Just click the "Join" button to connect to the game session.</b>
<br><br>
Optionally, you may enter a short nickname for yourself. This shows up on the high scores board that all players can see.
</p>
<div id="notLoggedIn">
<input type="text" maxlength="12" id="myNickname"/> <i>(optional nickname)</i>
<br><br>
<input type=button id="startNewGame" class="helpButtons" onclick="startNewGame()" value="Join Game" />
</div>
<div id="loggedIn">
<i>Logged in as <b><div id="myUserId"></div></b></i>
<br><br>
<input type=button id="quitGame" class="helpButtons" onclick="quitGame()" value="Quit Game" />
</div>
<br>
<p>
<b>Rules and tips</b>
<ol>
<li>You have infinite money.</li>
<li>Short selling is permitted.</li>
<li>Market orders are allowed. The unfilled portion is eliminated.</li>
<li>You can have 2 working limit orders on each side of the book. Enter a price within 5% of the last trade price.</li>
<li>Place orders in multiples of the 100, the round lot size. The maximum order size is 1,000 shares.</li>
<li>You may hold a maximum of 20,000 shares in a long or short position.</li>
<li>Market Maker and Price Taker bots are also playing, so keep an eye out for them!</li>
<li>Watch the message window for helpful tips.</li>
</ol>
<b>I recommend you use a modern browser such as Chrome, Firefox or Safari to play this game.</b> 
</p>
<hr>
Follow me on Twitter @<a href="http://www.twitter.com/htmlartzone">htmlartzone</a>; or write to me at <a href="mailto:arvindrao.dev@gmail.com?Subject=Trading HYP">arvindrao.dev@gmail.com</a>
<br><br>
I also have a <a href="http://tradinghyp.blogspot.com" target="_blank">technical blog</a> with more details on the design of this game.
<br><br>
<p>
<table border=0 cellpadding=0 cellspacing=0>
<tr>
<td>
<iframe src="http://www.facebook.com/plugins/like.php?href=http%3A%2F%2Ftradinghyp.appspot.com&amp;send=false&amp;layout=button_count&amp;width=100&amp;show_faces=false&amp;action=like&amp;colorscheme=light&amp;font=arial&amp;height=21" scrolling="no" frameborder="0" style="border:none; overflow:hidden; width:100px; height:21px;" allowTransparency="true"></iframe>
</td>
<td>
<a href="http://twitter.com/share" class="twitter-share-button" data-text="Check this out!" data-count="horizontal">Tweet</a><script type="text/javascript" src="http://platform.twitter.com/widgets.js"></script>
</td>
<td>
<!-- Place this tag where you want the +1 button to render -->
<g:plusone size="medium" href="http://tradinghyp.appspot.com"></g:plusone>

<!-- Place this tag after the last plusone tag -->
<script type="text/javascript">
  (function() {
    var po = document.createElement('script'); po.type = 'text/javascript'; po.async = true;
    po.src = 'https://apis.google.com/js/plusone.js';
    var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(po, s);
  })();
</script>
</td>
<td>
<script src="http://platform.linkedin.com/in.js" type="text/javascript"></script><script type="IN/Share" data-url="http://tradinghyp.appspot.com" data-counter="right"></script>
</td>
</tr>
</table>
</p>
<br>
<div id="finePrint">
<b>Disclaimer</b><br>
This website is for entertainment purposes only and does not provide any trading or investment advice. Real money is not used on this website.
<br>All stocks, companies, persons, prices and news items appearing in the game are fictional.
<br>
<br>
<a rel="license" href="http://creativecommons.org/licenses/by/3.0/"><img alt="Creative Commons License" style="border-width:0" src="http://i.creativecommons.org/l/by/3.0/80x15.png" /></a><br>This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by/3.0/">Creative Commons Attribution 3.0 Unported License</a>.
</div>

</div>
</div>
</body>


</html>

