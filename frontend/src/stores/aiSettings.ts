import {defineStore} from 'pinia'
import {clearAiKey,getAiSettings,saveAiSettings,testAi,type AiSettings,type AiSettingsUpdate} from '../api/study'
export type AiSettingsForm=AiSettingsUpdate
export const useAiSettingsStore=defineStore('ai-settings',{state:()=>({view:undefined as AiSettings|undefined,busy:false,status:''}),actions:{
 async load(){this.view=await getAiSettings()},
 async save(form:AiSettingsForm){this.busy=true;try{this.view=await saveAiSettings(form);this.status='设置已保存，密钥输入已从页面清除'}catch{this.status='保存失败，请检查地址、模型与额度';throw new Error(this.status)}finally{this.busy=false}},
 async clear(){await clearAiKey();this.status='密钥已清除';await this.load()},
 async test(){this.busy=true;try{this.status=(await testAi()).message}catch{this.status='连接测试失败'}finally{this.busy=false}},
}})
