import MarkdownIt from 'markdown-it'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js/lib/core'
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'
import java from 'highlight.js/lib/languages/java'
import json from 'highlight.js/lib/languages/json'
import xml from 'highlight.js/lib/languages/xml'
import sql from 'highlight.js/lib/languages/sql'
import yaml from 'highlight.js/lib/languages/yaml'

for (const [name, language] of Object.entries({javascript,typescript,java,json,xml,sql,yaml})) hljs.registerLanguage(name, language)
const escape=(value:string)=>value.replace(/[&<>"']/g,char=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]!))
const md=new MarkdownIt({html:true,linkify:true,typographer:true,highlight:(code,language)=>highlight(code,language)})
const highlight=(code:string,language:string)=>{let value:string;try{value=language&&hljs.getLanguage(language)?hljs.highlight(code,{language}).value:hljs.highlightAuto(code).value}catch{value=escape(code)}return `<pre class="hljs code-block"><code>${value.split('\n').map((line,index)=>`<span class="code-line" data-line="${index+1}">${line||' '}</span>`).join('\n')}</code></pre>`}
export function renderDocument(language:string,source:string){const unsafe=language==='markdown'?md.render(source):highlight(source,language);return String(DOMPurify.sanitize(unsafe,{USE_PROFILES:{html:true}}))}
export async function copyCode(source:string){await navigator.clipboard.writeText(source)}
