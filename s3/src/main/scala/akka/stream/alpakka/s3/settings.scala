/*
 * Copyright (C) 2016-2019 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.s3

import java.nio.file.{Path, Paths}
import java.util.Objects

import scala.util.Try
import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import com.amazonaws.auth._
import com.amazonaws.regions.{AwsRegionProvider, DefaultAwsRegionProviderChain}
import com.typesafe.config.Config

import scala.compat.java8.OptionConverters._

final class Proxy private (
    val host: String,
    val port: Int,
    val scheme: String
) {

  /** Java API */
  def getHost: String = host

  /** Java API */
  def getPort: Int = port

  /** Java API */
  def getScheme: String = scheme

  def withHost(value: String): Proxy = copy(host = value)
  def withPort(value: Int): Proxy = copy(port = value)
  def withScheme(value: String): Proxy = copy(scheme = value)

  private def copy(host: String = host, port: Int = port, scheme: String = scheme): Proxy =
    new Proxy(host = host, port = port, scheme = scheme)

  override def toString =
    "Proxy(" +
    s"host=$host," +
    s"port=$port," +
    s"scheme=$scheme" +
    ")"

  override def equals(other: Any): Boolean = other match {
    case that: Proxy =>
      Objects.equals(this.host, that.host) &&
      Objects.equals(this.port, that.port) &&
      Objects.equals(this.scheme, that.scheme)
    case _ => false
  }

  override def hashCode(): Int =
    Objects.hash(host, Int.box(port), scheme)
}

object Proxy {

  /** Scala API */
  def apply(host: String, port: Int, scheme: String): Proxy =
    new Proxy(host, port, scheme)

  /** Java API */
  def create(host: String, port: Int, scheme: String): Proxy =
    apply(host, port, scheme)
}

sealed abstract class ApiVersion
object ApiVersion {
  sealed abstract class ListBucketVersion1 extends ApiVersion
  case object ListBucketVersion1 extends ListBucketVersion1

  /** Java Api */
  def getListBucketVersion1: ListBucketVersion1 = ListBucketVersion1

  sealed abstract class ListBucketVersion2 extends ApiVersion
  case object ListBucketVersion2 extends ListBucketVersion2

  /** Java Api */
  def getListBucketVersion2: ListBucketVersion2 = ListBucketVersion2
}

final class S3Settings private (
    val bufferType: BufferType,
    val proxy: Option[Proxy],
    val credentialsProvider: AWSCredentialsProvider,
    val s3RegionProvider: AwsRegionProvider,
    val pathStyleAccess: Boolean,
    val endpointUrl: Option[String],
    val listBucketApiVersion: ApiVersion
) {

  /** Java API */
  def getBufferType: BufferType = bufferType

  /** Java API */
  def getProxy: java.util.Optional[Proxy] = proxy.asJava

  /** Java API */
  def getCredentialsProvider: AWSCredentialsProvider = credentialsProvider

  /** Java API */
  def getS3RegionProvider: AwsRegionProvider = s3RegionProvider

  /** Java API */
  def isPathStyleAccess: Boolean = pathStyleAccess

  /** Java API */
  def getEndpointUrl: java.util.Optional[String] = endpointUrl.asJava

  /** Java API */
  def getListBucketApiVersion: ApiVersion = listBucketApiVersion

  def withBufferType(value: BufferType): S3Settings = copy(bufferType = value)
  def withProxy(value: Proxy): S3Settings = copy(proxy = Option(value))
  def withCredentialsProvider(value: AWSCredentialsProvider): S3Settings =
    copy(credentialsProvider = value)
  def withS3RegionProvider(value: AwsRegionProvider): S3Settings = copy(s3RegionProvider = value)
  def withPathStyleAccess(value: Boolean): S3Settings =
    if (pathStyleAccess == value) this else copy(pathStyleAccess = value)
  def withEndpointUrl(value: String): S3Settings = copy(endpointUrl = Option(value))
  def withListBucketApiVersion(value: ApiVersion): S3Settings =
    copy(listBucketApiVersion = value)

  private def copy(
      bufferType: BufferType = bufferType,
      proxy: Option[Proxy] = proxy,
      credentialsProvider: AWSCredentialsProvider = credentialsProvider,
      s3RegionProvider: AwsRegionProvider = s3RegionProvider,
      pathStyleAccess: Boolean = pathStyleAccess,
      endpointUrl: Option[String] = endpointUrl,
      listBucketApiVersion: ApiVersion = listBucketApiVersion
  ): S3Settings = new S3Settings(
    bufferType = bufferType,
    proxy = proxy,
    credentialsProvider = credentialsProvider,
    s3RegionProvider = s3RegionProvider,
    pathStyleAccess = pathStyleAccess,
    endpointUrl = endpointUrl,
    listBucketApiVersion = listBucketApiVersion
  )

  override def toString =
    "S3Settings(" +
    s"bufferType=$bufferType," +
    s"proxy=$proxy," +
    s"credentialsProvider=$credentialsProvider," +
    s"s3RegionProvider=$s3RegionProvider," +
    s"pathStyleAccess=$pathStyleAccess," +
    s"endpointUrl=$endpointUrl," +
    s"listBucketApiVersion=$listBucketApiVersion" +
    ")"

  override def equals(other: Any): Boolean = other match {
    case that: S3Settings =>
      java.util.Objects.equals(this.bufferType, that.bufferType) &&
      Objects.equals(this.proxy, that.proxy) &&
      Objects.equals(this.credentialsProvider, that.credentialsProvider) &&
      Objects.equals(this.s3RegionProvider, that.s3RegionProvider) &&
      Objects.equals(this.pathStyleAccess, that.pathStyleAccess) &&
      Objects.equals(this.endpointUrl, that.endpointUrl) &&
      Objects.equals(this.listBucketApiVersion, that.listBucketApiVersion)
    case _ => false
  }

  override def hashCode(): Int =
    Objects.hash(bufferType,
                 proxy,
                 credentialsProvider,
                 s3RegionProvider,
                 Boolean.box(pathStyleAccess),
                 endpointUrl,
                 listBucketApiVersion)
}

object S3Settings {
  val ConfigPath = "alpakka.s3"

  /**
   * Reads from the given config.
   */
  def apply(c: Config): S3Settings = {

    val bufferType = c.getString("buffer") match {
      case "memory" =>
        MemoryBufferType

      case "disk" =>
        val diskBufferPath = c.getString("disk-buffer-path")
        DiskBufferType(Paths.get(diskBufferPath))

      case other =>
        throw new IllegalArgumentException(s"Buffer type must be 'memory' or 'disk'. Got: [$other]")
    }

    val maybeProxy = for {
      host ← Try(c.getString("proxy.host")).toOption if host.nonEmpty
    } yield {
      Proxy(
        host,
        c.getInt("proxy.port"),
        Uri.httpScheme(c.getBoolean("proxy.secure"))
      )
    }

    val pathStyleAccess = c.getBoolean("path-style-access")

    val endpointUrl = if (c.hasPath("endpoint-url")) {
      Option(c.getString("endpoint-url"))
    } else {
      None
    }

    val regionProvider = {
      val regionProviderPath = "aws.region.provider"

      val staticRegionProvider = new AwsRegionProvider {
        lazy val getRegion: String = c.getString("aws.region.default-region")
      }

      if (c.hasPath(regionProviderPath)) {
        c.getString(regionProviderPath) match {
          case "static" =>
            staticRegionProvider

          case _ =>
            new DefaultAwsRegionProviderChain()
        }
      } else {
        new DefaultAwsRegionProviderChain()
      }
    }

    val credentialsProvider = {
      val credProviderPath = "aws.credentials.provider"

      if (c.hasPath(credProviderPath)) {
        c.getString(credProviderPath) match {
          case "default" ⇒
            DefaultAWSCredentialsProviderChain.getInstance()

          case "static" ⇒
            val aki = c.getString("aws.credentials.access-key-id")
            val sak = c.getString("aws.credentials.secret-access-key")
            val tokenPath = "aws.credentials.token"
            val creds = if (c.hasPath(tokenPath)) {
              new BasicSessionCredentials(aki, sak, c.getString(tokenPath))
            } else {
              new BasicAWSCredentials(aki, sak)
            }
            new AWSStaticCredentialsProvider(creds)

          case "anon" ⇒
            new AWSStaticCredentialsProvider(new AnonymousAWSCredentials())

          case _ ⇒
            DefaultAWSCredentialsProviderChain.getInstance()
        }
      } else {
        DefaultAWSCredentialsProviderChain.getInstance()
      }
    }

    val apiVersion = Try(c.getInt("list-bucket-api-version") match {
      case 1 => ApiVersion.ListBucketVersion1
      case 2 => ApiVersion.ListBucketVersion2
    }).getOrElse(ApiVersion.ListBucketVersion2)

    new S3Settings(
      bufferType = bufferType,
      proxy = maybeProxy,
      credentialsProvider = credentialsProvider,
      s3RegionProvider = regionProvider,
      pathStyleAccess = pathStyleAccess,
      endpointUrl = endpointUrl,
      listBucketApiVersion = apiVersion
    )
  }

  /**
   * Java API: Reads from the given config.
   */
  def create(c: Config): S3Settings = apply(c)

  /** Scala API */
  def apply(
      bufferType: BufferType,
      proxy: Option[Proxy],
      credentialsProvider: AWSCredentialsProvider,
      s3RegionProvider: AwsRegionProvider,
      pathStyleAccess: Boolean,
      endpointUrl: Option[String],
      listBucketApiVersion: ApiVersion
  ): S3Settings = new S3Settings(
    bufferType,
    proxy,
    credentialsProvider,
    s3RegionProvider,
    pathStyleAccess,
    endpointUrl,
    listBucketApiVersion
  )

  /** Java API */
  def create(
      bufferType: BufferType,
      proxy: java.util.Optional[Proxy],
      credentialsProvider: AWSCredentialsProvider,
      s3RegionProvider: AwsRegionProvider,
      pathStyleAccess: Boolean,
      endpointUrl: java.util.Optional[String],
      listBucketApiVersion: ApiVersion
  ): S3Settings = apply(
    bufferType,
    proxy.asScala,
    credentialsProvider,
    s3RegionProvider,
    pathStyleAccess,
    endpointUrl.asScala,
    listBucketApiVersion
  )

  /**
   * Scala API: Creates [[S3Settings]] from the [[com.typesafe.config.Config Config]] attached to an [[akka.actor.ActorSystem]].
   */
  def apply()(implicit system: ActorSystem): S3Settings = apply(system.settings.config.getConfig(ConfigPath))

  /**
   * Java API: Creates [[S3Settings]] from the [[com.typesafe.config.Config Config]] attached to an [[akka.actor.ActorSystem]].
   */
  def create(system: ActorSystem): S3Settings = apply()(system)
}

sealed trait BufferType {
  def path: Option[Path]

  /** Java API */
  def getPath: java.util.Optional[Path] = path.asJava
}

case object MemoryBufferType extends BufferType {
  def getInstance: BufferType = MemoryBufferType
  override def path: Option[Path] = None
}

final class DiskBufferType private (filePath: Path) extends BufferType {
  override val path: Option[Path] = Some(filePath).filterNot(_.toString.isEmpty)
}
case object DiskBufferType {
  def apply(path: Path): DiskBufferType = new DiskBufferType(path)

  /** Java API */
  def create(path: Path): DiskBufferType = DiskBufferType(path)
}
