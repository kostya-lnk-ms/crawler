package crawler

import java.io.InputStream
import java.net.{MalformedURLException, URI, URISyntaxException, URL}
import org.htmlcleaner.HtmlCleaner

import scala.collection.concurrent.TrieMap
import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Try}

object Crawler {

  case class Link(url: URL, description: String)
  case class Image(url: URL, description: String)

  case class Page(url: URL, title: Option[String], links: Seq[Link], images: Seq[Image])

  def apply(url: URL) = new Crawler(url)
}

class Crawler(val baseUrl: URL, urlOpen: URL => InputStream = (u)=> u.openStream ) {
  import Crawler._

  val seen = TrieMap[URL, Boolean]()

  private def effectiveUrl(base: URL, to: String): URL = {
    def getPathOnly(url: URL) = {
      url.getPath.lastIndexOf('/') match {
        case -1 => new URL(url.toExternalForm + "/")
        case p => new URL(url.getProtocol, url.getHost, url.getPort, url.getPath.substring(0, p + 1))
      }
    }
    val toUrl = try {
      if (to.isEmpty)
        Some(base)
      else
        Some(new URL(to))
    } catch {
      case _ : MalformedURLException => None
    }
    toUrl getOrElse {
      val res = if (to(0) == '#') {
        base.getRef match {
          case null => new URL(base.toExternalForm + to)
          case _ =>
            val uS = base.toExternalForm
            new URL( uS.substring(0, uS.indexOf('#')) + to )
          }
      }
      else if (to(0) == '/') new URL(base.getProtocol, base.getHost, base.getPort, to)
      else new URL( getPathOnly(base).toExternalForm + to )
      try {
        new URI(res.toExternalForm).normalize.toURL
      } catch {
        case _ : URISyntaxException => res
      }
    }
  }

  private def canFollow(link: Link) = {
    link.url.getHost == baseUrl.getHost
  }

  private def parsePageFromUrl(url: URL): Future[Page] = {
    import scala.collection.JavaConverters._
    val cleaner = new HtmlCleaner
    Future {
      val rootNode = cleaner.clean( urlOpen(url) )
      new Page(url,
        rootNode.getElementListByName("title", true).asScala.headOption.map{_.getText.toString},
        rootNode.getElementsByName("a", true).filter(_.getAttributeByName("href") != null).map {
          el => Link(effectiveUrl(url, el.getAttributeByName("href")), el.getText.toString)
        },
        rootNode.getElementsByName("img", true).filter(_.getAttributeByName("src") != null).map {
          el => Image(effectiveUrl(url, el.getAttributeByName("src")),
                      el.getAttributeByName("alt") match {
                        case null => ""
                        case a => a
                      })
        }
      )
    }

  }

  def hostMap: Future[Seq[Page]] = {

    def doHostMap(url: URL): Future[Seq[Page]] = {
      seen.putIfAbsent(url, true)
      println(url)
      if (url.getProtocol != "http" && url.getProtocol != "file")
        Promise.successful(Nil).future
      else
        parsePageFromUrl(url).flatMap { page =>
          val s = page.links.filter { link=>
            canFollow(link) && seen.putIfAbsent(link.url, true).isEmpty
          }.map { link => doHostMap(link.url) }
          val s1 = Future.sequence(s).map(ss=> ss.flatten)
          s1.map(ss => Seq(page) ++ ss )
        }
    }

    doHostMap(baseUrl)
  }

  def hostMapSync(limit: FiniteDuration) : Try[ Seq[Page] ] = {
    val r = hostMap
    Await.ready(r, limit)
    r.value.getOrElse( Failure(new RuntimeException(s"Did not complete within the $limit")) )
  }
}
