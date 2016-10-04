package crawler

import java.io.{FileOutputStream, PrintStream}
import java.net.URL

import scala.language.postfixOps
import scala.util.{Failure, Success}
import scala.concurrent.duration._

object CrawlerApp extends App {

  val TIME_LIMIT = 59 seconds

  def run = {
    if (args.length < 1)
      sys.error("No URL supplied")
    else
    {
      val baseURL = new URL(args(0))
      println(s"Crawling the ${baseURL.toExternalForm}, time limit is ${TIME_LIMIT}")
      val crawler = Crawler( baseURL )
      crawler.hostMapSync(TIME_LIMIT) match
      {
        case Success(pages)=>
          val out = if (args.length == 2)
            new PrintStream( new FileOutputStream(args(1)) )
          else
            System.out
          out.print( MapRenderer.render(baseURL, pages) )
          out.flush
        case Failure(er)=> sys.error(er.toString)
      }
    }
  }

  run

}
