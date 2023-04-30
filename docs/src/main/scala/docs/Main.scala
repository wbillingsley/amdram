package docs

import com.wbillingsley.veautiful.html.{Attacher, StyleSuite, Styling, <}
import com.wbillingsley.veautiful.doctacular

import org.scalajs.dom

/** This is our own set of styles for widgets that we define */
given siteStyles:StyleSuite = StyleSuite()
val site = doctacular.Site()

val latestVersion = "0.3.0"

/** A style class for embedded examples */
val embeddedExampleStyle = Styling(
  """background: antiquewhite;
    |padding: 10px;
    |border-radius: 10px;
    |margin-bottom: 1rem;
    |""".stripMargin).register()

object Main {

  def main(args:Array[String]): Unit = {
    import site.given
    import doctacular.{*, given}
    
    // To set the theme colours, we're rudely adding rules to the CSS that the Site's layout engine produces
    site.pageLayout.leftSideBarStyle.addRules(
      """
        |background: aliceblue;
        |border: none;
        |""".stripMargin)

    site.pageLayout.sideBarToggleStyle.addRules(
      """
        |background: aliceblue;
        |border: none;
        |""".stripMargin)

    site.pageLayout.contentStyle.addRules(
      " pre" -> "background: aliceblue; padding: 10px; border-radius: 10px;",
      " h1,h2,h3,h4" -> "font-family: 'Times New Roman', serif; color: #004479; font-style: italic; margin-top: 2rem;"
    )
    
    // The TOC is the table of contents in teh left side-bar.
    // In this case, at the same time as setting the TOC, we're also adding the pages to the site's router.
    site.toc = site.Toc(
      site.TocNodeLink(<.h1("Amdram"), site.HomeRoute),

      "ping-pong" ->site.addPage("Ping Pong", pingpong)
    )
    
    // The site's home page is set separately, because many sites have more of a landing page (e.g. without a sidebar)
    // In this case, though, we just tell it to render the Intro page as the homepage.
    site.home = () => site.renderPage(home)
    
    // Install our custom CSS - note this is the stylings we've created in our custom widgets (rather than the 
    // sidebar styles, which the site will install when we attach it to the DOM)
    siteStyles.install()
    
    // Render our site into the page 
    site.attachTo(dom.document.body)
  }

}
