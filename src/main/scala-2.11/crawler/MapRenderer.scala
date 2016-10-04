package crawler

import java.net.URL

object MapRenderer {
  import Crawler._

  def render(baseURL: URL, pageInfo: Seq[Page]) = {
    def images(page: Page) = {
      if (page.images.nonEmpty) {
        Seq(<h3>Images:</h3>) ++
        page.images.map { img => <p>
          {img.description} <a href={img.url.toExternalForm}>{img.url}</a>
        </p>
        }
      } else Nil
    }
    def links(page: Page) = {
      if (page.links.nonEmpty) {
        Seq(<h3>Links:</h3>) ++
        page.links.map { link => <p>
          {link.description} <a href={link.url.toExternalForm}>{link.url}</a>
        </p>
        }
      } else Nil
    }


    <html>
      <head>
        <title>Map of the {baseURL.toExternalForm}</title>
      </head>
      <body>
        <h1>Map of the {baseURL.toExternalForm}</h1>
        <p>{
          pageInfo.map { p=>
            <h2>{p.title.getOrElse("")} { p.url.toExternalForm }</h2>
              <p>{ links(p) } </p>
              <p>{ images(p) }</p>
          }
        }</p>
      </body>
    </html>
  }

}
