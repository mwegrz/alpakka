/*
 * Copyright (C) 2016-2019 Lightbend Inc. <http://www.lightbend.com>
 */

package docs.scaladsl
import java.io.PrintWriter
import java.net.InetAddress

import akka.stream.Materializer
import akka.stream.alpakka.ftp.{FtpBaseSupport, FtpSettings, PlainFtpSupportImpl}
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.StreamTestKit.assertAllStagesStopped
import akka.testkit.TestKit
import org.apache.commons.net.PrintCommandListener
import org.apache.commons.net.ftp.FTPClient
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class FtpExamplesSpec
    extends PlainFtpSupportImpl
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures {

  implicit val materializer: Materializer = getMaterializer

  val hostname = FtpBaseSupport.hostname
  def port = getPort

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    startServer()
  }

  override protected def afterAll(): Unit = {
    stopServer()
    TestKit.shutdownActorSystem(getSystem)
    super.afterAll()
  }

  def ftpSettings = {
    //#create-settings
    val ftpSettings = FtpSettings
      .create(InetAddress.getByName(hostname))
      .withPort(port)
      .withBinary(true)
      .withPassiveMode(true)
      // only useful for debugging
      .withConfigureConnection((ftpClient: FTPClient) => {
        ftpClient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out), true))
      })
    //#create-settings
    ftpSettings
  }

  "a file" should {
    "be stored" in assertAllStagesStopped {
      //#storing
      import akka.stream.IOResult
      import akka.stream.alpakka.ftp.scaladsl.Ftp
      import akka.util.ByteString
      import scala.concurrent.Future

      val result: Future[IOResult] = Source
        .single(ByteString("this is the file contents"))
        .runWith(Ftp.toPath("file.txt", ftpSettings))
      //#storing

      val ioResult = result.futureValue
      ioResult should be(IOResult.createSuccessful(25))

      val p = fileExists(FtpBaseSupport.FTP_ROOT_DIR, "file.txt")
      p should be(true)

    }

    "be gzipped" in assertAllStagesStopped {
      import akka.stream.IOResult
      import akka.stream.alpakka.ftp.scaladsl.Ftp
      import akka.util.ByteString
      import scala.concurrent.Future

      //#storing

      // Create a gzipped target file
      import akka.stream.scaladsl.Compression
      val result: Future[IOResult] = Source
        .single(ByteString("this is the file contents" * 50))
        .via(Compression.gzip)
        .runWith(Ftp.toPath("file.txt.gz", ftpSettings))
      //#storing

      val ioResult = result.futureValue
      ioResult should be(IOResult.createSuccessful(61))

      val p = fileExists(FtpBaseSupport.FTP_ROOT_DIR, "file.txt.gz")
      p should be(true)

    }
  }

}
