// Actual non-OP player interactions, restricted to the owned loopback QA world.
const fs=require('fs'),path=require('path'),root=path.resolve(__dirname,'..');
const mineflayer=require(path.join(root,'.runtime/client/node_modules/mineflayer'));
const {Vec3}=require(path.join(root,'.runtime/client/node_modules/vec3'));
const target=JSON.parse(process.env.QA_GRIEF_TARGET);
const bot=mineflayer.createBot({host:'127.0.0.1',port:25578,username:'WinterGrief',auth:'offline',version:'1.21.11'});
const delay=ms=>new Promise(r=>setTimeout(r,ms));
const result={minecraft:'1.21.11',scope:'Non-OP survival player; naturally generated decoration in isolated loopback world. Console supplies stone, teleport and TNT only.',target,checks:[]};let completed=false,started=false;
function check(name,ok,detail){result.checks.push({name,status:ok?'PASS':'FAIL',detail});if(!ok)throw Error(name+': '+detail);}
const timeout=setTimeout(()=>finish(Error('Grief timeout')),90000);
function finish(error){if(completed)return;completed=true;clearTimeout(timeout);if(error)result.error=String(error.stack||error);fs.writeFileSync(path.join(root,'reports',process.env.QA_GRIEF_REPORT||'anarchy05-grief-client.json'),JSON.stringify(result,null,2)+'\n');console.log(error?'GRIEF_FAILED '+error:'GRIEF_DONE');bot.quit();setTimeout(()=>process.exit(error?1:0),250);}
bot.on('error',finish);bot.on('kicked',r=>finish(Error(JSON.stringify(r))));bot.once('spawn',()=>console.log('GRIEF_READY'));
bot.on('messagestr',async message=>{
 if(!message.includes('GRIEF_GO')||started||completed)return;started=true;
 try{
  await delay(1800);check('survival_mode',bot.game.gameMode==='survival',bot.game.gameMode);
  const location=new Vec3(...target.block),block=bot.blockAt(location);
  check('natural_decor_present',block&&block.name===target.material,block?.name);
  await bot.dig(block);await delay(600);check('natural_decor_breakable',bot.blockAt(location)?.name==='air',bot.blockAt(location)?.name);
  const stone=bot.inventory.items().find(i=>i.name==='stone');if(!stone)throw Error('QA supply missing');await bot.equip(stone,'hand');
  const below=bot.blockAt(location.offset(0,-1,0));await bot.placeBlock(below,new Vec3(0,1,0));await delay(600);
  check('player_build_allowed',bot.blockAt(location)?.name==='stone',bot.blockAt(location)?.name);
  console.log('GRIEF_TNT_READY');await delay(8000);
  check('explosion_changes_decoration',bot.blockAt(location)?.name!=='stone',bot.blockAt(location)?.name);
  finish();
 }catch(error){finish(error);}
});
