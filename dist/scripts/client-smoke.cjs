// A real local Minecraft protocol client; only for the isolated loopback QA server.
const fs = require('fs');
const path = require('path');
const root = path.resolve(__dirname, '..');
const mineflayer = require(path.join(root, '.runtime/client/node_modules/mineflayer'));
const bot = mineflayer.createBot({host:'127.0.0.1',port:Number(process.env.QA_PORT||25576),username:'WinterQA',auth:'offline',version:'1.21.11'});
const delay = ms => new Promise(r=>setTimeout(r,ms));
const result = {version:'1.21.11',client:'mineflayer',scope:'isolated loopback QA server with offline authentication',checks:[]};
let completed=false;
const timeout=setTimeout(()=>finish(new Error('Client smoke timeout')),90000);
function check(name,passed,detail) {result.checks.push({name,status:passed?'PASS':'FAIL',detail});if(!passed)throw new Error(name+': '+detail);}
function finish(error) {
 if(completed)return;completed=true;clearTimeout(timeout);
 if(error)result.error=String(error.stack||error);
 fs.writeFileSync(path.join(root,'reports',process.env.QA_REPORT||'client-smoke.json'),JSON.stringify(result,null,2)+'\n');
 console.log(error?'CLIENT_FAILED '+error:'CLIENT_DONE');bot.quit();setTimeout(()=>process.exit(error?1:0),250);
}
bot.on('error',finish);bot.on('kicked',reason=>finish(new Error('Kicked '+JSON.stringify(reason))));
bot.once('spawn',()=>{console.log('CLIENT_READY');});
bot.on('messagestr', async message=>{
 if(!message.includes('QA_GO')||completed||result.started)return;
 result.started=true;
 try {
  if(!process.env.QA_TARGET)bot.chat('/tp 200011 161 200008');await delay(1500);
  if(!process.env.QA_TARGET)bot.chat('/bair start default');await delay(6500);
  const location=bot.entity.position.floored().offset(-3,0,0);if(process.env.QA_TARGET){const t=JSON.parse(process.env.QA_TARGET);location.x=t.x;location.y=t.y;location.z=t.z;}
  const block=bot.blockAt(location);
  check('real_luna_block_appears',block&&block.name==='ender_chest',block?block.name:'missing chunk');
  const window=await bot.openContainer(block);
  check('real_client_opens_luna_inventory',window.inventoryStart===54,`type=${window.type}, loot slots=${window.inventoryStart}`);
  result.lootItems=window.slots.slice(0,window.inventoryStart).filter(Boolean).map(i=>({name:i.name,count:i.count}));
  await delay(500);bot.closeWindow(window);bot.chat('/bair stop default');await delay(2000);
  check('real_luna_cleanup',bot.blockAt(location)?.name==='air',bot.blockAt(location)?.name);
  finish();
 } catch(error){finish(error);}
});
