package crawler

import java.io.{ByteArrayInputStream}
import java.net.URL
import scala.concurrent.duration._
import scala.language.postfixOps

import org.scalatest.FunSuite

class CrawlerTest extends FunSuite {

  val fb = <html>
    <a href="http://twitter.com"></a>
    <a href="http://base.com/1"></a>
  </html>

  val f1 = <html>
    <a href="http://base.com"></a>
    <img src="a.jpg"></img>
    <a href="dummy/../2">eee</a>
  </html>

  val f2 = <html>
    <img src="z.gif"></img>
  </html>

  test("testApply") {
    val c = new Crawler(new URL("http://base.com"), u=> {
      assert(u.getHost === "base.com")
      if (u.getPath == "/1")
        new ByteArrayInputStream(f1.toString.getBytes)
      else if (u.getPath == "/2")
        new ByteArrayInputStream(f2.toString.getBytes)
      else
        new ByteArrayInputStream(fb.toString.getBytes)
    })
    val res = c.hostMapSync(3 seconds)
    assert(res.isSuccess)
    val r = res.get
    assert(r.size === 3)
    val allLinks = r.flatMap(_.links)
    assert(allLinks.size === 4)
    assert( allLinks.find( _.url.toExternalForm == "http://base.com/2" ).isDefined  )
    val allImages = r.flatMap(_.images)
    assert(allImages.size === 2)
  }

}
