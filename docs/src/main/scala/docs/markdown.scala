package docs

import com.wbillingsley.veautiful.html.{<, Markup, SVG, VDomNode, ^}

import scala.scalajs.js
import scala.scalajs.js.annotation._

@js.native
@JSImport("highlight.js", JSImport.Default)
object HLJS extends js.Object:
  def highlight(code:String, d:js.Dictionary[String]):js.Dynamic = js.native
  def getLanguage(lang:String):js.Dynamic = js.native

@js.native
@JSImport("marked", "marked")
object Marked extends js.Object:
  def parse(s:String, d:js.Dictionary[js.Function]):String = js.native
  def parseInline(s:String):String = js.native

given marked:Markup = Markup({ (s:String) => Marked.parse(s, js.Dictionary("highlight" -> 
    { (code:String, lang:String) => 
      import scalajs.js.DynamicImplicits.truthValue
      val l = if HLJS.getLanguage(lang) then lang else "plaintext"
      HLJS.highlight(code, js.Dictionary("language" -> l)).value 
    }
  )) 
})