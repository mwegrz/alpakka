/*
 * Copyright (C) 2016-2019 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.ftp

import java.net.InetAddress
import java.nio.file.attribute.PosixFilePermission

import akka.annotation.InternalApi
import org.apache.commons.net.ftp.{FTPClient, FTPSClient}

/**
 * FTP remote file descriptor.
 *
 * @param name file name
 * @param path remote file path as viewed by the logged user.
 *             It should always start by '/'
 * @param isDirectory the descriptor is a directory
 * @param size the file size in bytes
 * @param lastModified the timestamp of the file last modification
 * @param permissions the permissions of the file
 */
final case class FtpFile(
    name: String,
    path: String,
    isDirectory: Boolean,
    size: Long,
    lastModified: Long,
    permissions: Set[PosixFilePermission]
) {
  val isFile: Boolean = !this.isDirectory
}

/**
 * Common remote file settings.
 */
sealed abstract class RemoteFileSettings {
  def host: InetAddress
  def port: Int
  def credentials: FtpCredentials
}

/**
 * Common settings for FTP and FTPs.
 */
sealed abstract class FtpFileSettings extends RemoteFileSettings {
  def binary: Boolean // BINARY or ASCII (default)
  def passiveMode: Boolean
}

/**
 * FTP settings
 *
 * @param host host
 * @param port port
 * @param credentials credentials (username and password)
 * @param binary specifies the file transfer mode, BINARY or ASCII. Default is ASCII (false)
 * @param passiveMode specifies whether to use passive mode connections. Default is active mode (false)
 * @param configureConnection A function which will be called after connecting to the server. Use this for
 *                            any custom configuration required by the server you are connecting to.
 */
final class FtpSettings private (
    val host: java.net.InetAddress,
    val port: Int,
    val credentials: FtpCredentials,
    val binary: Boolean,
    val passiveMode: Boolean,
    val configureConnection: FTPClient => Unit
) extends FtpFileSettings {

  def withHost(value: java.net.InetAddress): FtpSettings = copy(host = value)
  def withPort(value: Int): FtpSettings = copy(port = value)
  def withCredentials(value: FtpCredentials): FtpSettings = copy(credentials = value)
  def withBinary(value: Boolean): FtpSettings = if (binary == value) this else copy(binary = value)
  def withPassiveMode(value: Boolean): FtpSettings = if (passiveMode == value) this else copy(passiveMode = value)

  /**
   * Scala API:
   * Sets the configure connection callback.
   */
  def withConfigureConnection(value: FTPClient => Unit): FtpSettings =
    copy(configureConnection = value)

  /**
   * Java API:
   * Sets the configure connection callback.
   */
  def withConfigureConnectionConsumer(configureConnection: java.util.function.Consumer[FTPClient]): FtpSettings =
    copy(configureConnection = configureConnection.accept)

  private def copy(
      host: java.net.InetAddress = host,
      port: Int = port,
      credentials: FtpCredentials = credentials,
      binary: Boolean = binary,
      passiveMode: Boolean = passiveMode,
      configureConnection: FTPClient => Unit = configureConnection
  ): FtpSettings = new FtpSettings(
    host = host,
    port = port,
    credentials = credentials,
    binary = binary,
    passiveMode = passiveMode,
    configureConnection = configureConnection
  )

  override def toString =
    s"""FtpSettings(host=$host,port=$port,credentials=$credentials,binary=$binary,passiveMode=$passiveMode,configureConnection=$configureConnection)"""
}

/**
 * FTP settings factory
 */
object FtpSettings {

  /** Default FTP port (21) */
  final val DefaultFtpPort = 21

  /** Scala API */
  def apply(
      host: java.net.InetAddress
  ): FtpSettings = new FtpSettings(
    host,
    port = DefaultFtpPort,
    credentials = FtpCredentials.AnonFtpCredentials,
    binary = false,
    passiveMode = false,
    configureConnection = _ => ()
  )

  /** Java API */
  def create(
      host: java.net.InetAddress
  ): FtpSettings = apply(
    host
  )
}

/**
 * FTPs settings
 *
 * @param host host
 * @param port port
 * @param credentials credentials (username and password)
 * @param binary specifies the file transfer mode, BINARY or ASCII. Default is ASCII (false)
 * @param passiveMode specifies whether to use passive mode connections. Default is active mode (false)
 * @param configureConnection A function which will be called after connecting to the server. Use this for
 *                            any custom configuration required by the server you are connecting to.
 */
final class FtpsSettings private (
    val host: java.net.InetAddress,
    val port: Int,
    val credentials: FtpCredentials,
    val binary: Boolean,
    val passiveMode: Boolean,
    val configureConnection: FTPSClient => Unit
) extends FtpFileSettings {

  def withHost(value: java.net.InetAddress): FtpsSettings = copy(host = value)
  def withPort(value: Int): FtpsSettings = copy(port = value)
  def withCredentials(value: FtpCredentials): FtpsSettings = copy(credentials = value)
  def withBinary(value: Boolean): FtpsSettings = if (binary == value) this else copy(binary = value)
  def withPassiveMode(value: Boolean): FtpsSettings = if (passiveMode == value) this else copy(passiveMode = value)

  /**
   * Scala API:
   * Sets the configure connection callback.
   */
  def withConfigureConnection(value: FTPSClient => Unit): FtpsSettings = copy(configureConnection = value)

  /**
   * Java API:
   * Sets the configure connection callback.
   */
  def withConfigureConnectionConsumer(configureConnection: java.util.function.Consumer[FTPSClient]): FtpsSettings =
    copy(configureConnection = configureConnection.accept)

  private def copy(
      host: java.net.InetAddress = host,
      port: Int = port,
      credentials: FtpCredentials = credentials,
      binary: Boolean = binary,
      passiveMode: Boolean = passiveMode,
      configureConnection: FTPSClient => Unit = configureConnection
  ): FtpsSettings = new FtpsSettings(
    host = host,
    port = port,
    credentials = credentials,
    binary = binary,
    passiveMode = passiveMode,
    configureConnection = configureConnection
  )

  override def toString =
    s"""FtpsSettings(host=$host,port=$port,credentials=$credentials,binary=$binary,passiveMode=$passiveMode,configureConnection=$configureConnection)"""
}

/**
 * FTPs settings factory
 */
object FtpsSettings {

  /** Default FTPs port (2222) */
  final val DefaultFtpsPort = 2222

  /** Scala API */
  def apply(
      host: java.net.InetAddress
  ): FtpsSettings = new FtpsSettings(
    host,
    DefaultFtpsPort,
    FtpCredentials.AnonFtpCredentials,
    binary = false,
    passiveMode = false,
    configureConnection = _ => ()
  )

  /** Java API */
  def create(
      host: java.net.InetAddress
  ): FtpsSettings = apply(
    host
  )
}

/**
 * SFTP settings
 *
 * @param host host
 * @param port port
 * @param credentials credentials (username and password)
 * @param strictHostKeyChecking sets whether to use strict host key checking.
 * @param knownHosts known hosts file to be used when connecting
 * @param sftpIdentity private/public key config to use when connecting
 */
final class SftpSettings private (
    val host: java.net.InetAddress,
    val port: Int,
    val credentials: FtpCredentials,
    val strictHostKeyChecking: Boolean,
    val knownHosts: Option[String],
    val sftpIdentity: Option[SftpIdentity]
) extends RemoteFileSettings {

  def withHost(value: java.net.InetAddress): SftpSettings = copy(host = value)
  def withPort(value: Int): SftpSettings = copy(port = value)
  def withCredentials(value: FtpCredentials): SftpSettings = copy(credentials = value)
  def withStrictHostKeyChecking(value: Boolean): SftpSettings =
    if (strictHostKeyChecking == value) this else copy(strictHostKeyChecking = value)
  def withKnownHosts(value: String): SftpSettings = copy(knownHosts = Option(value))
  def withSftpIdentity(value: SftpIdentity): SftpSettings = copy(sftpIdentity = Option(value))

  private def copy(
      host: java.net.InetAddress = host,
      port: Int = port,
      credentials: FtpCredentials = credentials,
      strictHostKeyChecking: Boolean = strictHostKeyChecking,
      knownHosts: Option[String] = knownHosts,
      sftpIdentity: Option[SftpIdentity] = sftpIdentity
  ): SftpSettings = new SftpSettings(
    host = host,
    port = port,
    credentials = credentials,
    strictHostKeyChecking = strictHostKeyChecking,
    knownHosts = knownHosts,
    sftpIdentity = sftpIdentity
  )

  override def toString =
    s"""SftpSettings(host=$host,port=$port,credentials=$credentials,strictHostKeyChecking=$strictHostKeyChecking,knownHosts=$knownHosts,sftpIdentity=$sftpIdentity)"""
}

/**
 * SFTP settings factory
 */
object SftpSettings {

  /** Default SFTP port (22) */
  final val DefaultSftpPort = 22

  /** Scala API */
  def apply(
      host: java.net.InetAddress
  ): SftpSettings = new SftpSettings(
    host,
    DefaultSftpPort,
    FtpCredentials.AnonFtpCredentials,
    strictHostKeyChecking = true,
    knownHosts = None,
    sftpIdentity = None
  )

  /** Java API */
  def create(
      host: java.net.InetAddress
  ): SftpSettings = apply(
    host
  )
}

/**
 * FTP credentials
 */
sealed abstract class FtpCredentials {
  def username: String
  def password: String
}

/**
 * FTP credentials factory
 */
object FtpCredentials {
  final val Anonymous = "anonymous"

  val anonymous: FtpCredentials = AnonFtpCredentials

  /**
   * Anonymous credentials
   */
  case object AnonFtpCredentials extends FtpCredentials {
    val username: String = Anonymous
    val password: String = Anonymous

    override def toString = "FtpCredentials(anonymous)"
  }

  /**
   * Non-anonymous credentials
   *
   * @param username the username
   * @param password the password
   */
  final class NonAnonFtpCredentials @InternalApi private[FtpCredentials] (val username: String, val password: String)
      extends FtpCredentials {
    override def toString = s"FtpCredentials(username=$username,password.nonEmpty=${password.nonEmpty})"
  }

  /** Create username/password credentials. */
  def create(username: String, password: String): FtpCredentials =
    new NonAnonFtpCredentials(username, password)
}

/**
 * SFTP identity details
 */
sealed abstract class SftpIdentity {
  type KeyType
  val privateKey: KeyType
  val privateKeyFilePassphrase: Option[Array[Byte]]
}

/**
 * SFTP identity factory
 */
object SftpIdentity {

  /**
   * SFTP identity for authenticating using private/public key value
   *
   * @param privateKey private key value to use when connecting
   */
  def createRawSftpIdentity(privateKey: Array[Byte]): RawKeySftpIdentity =
    new RawKeySftpIdentity(privateKey)

  /**
   * SFTP identity for authenticating using private/public key value
   *
   * @param privateKey private key value to use when connecting
   * @param privateKeyFilePassphrase password to use to decrypt private key
   */
  def createRawSftpIdentity(privateKey: Array[Byte], privateKeyFilePassphrase: Array[Byte]): RawKeySftpIdentity =
    new RawKeySftpIdentity(privateKey, Some(privateKeyFilePassphrase))

  /**
   * SFTP identity for authenticating using private/public key value
   *
   * @param privateKey private key value to use when connecting
   * @param privateKeyFilePassphrase password to use to decrypt private key
   * @param publicKey public key value to use when connecting
   */
  def createRawSftpIdentity(
      privateKey: Array[Byte],
      privateKeyFilePassphrase: Array[Byte],
      publicKey: Array[Byte]
  ): RawKeySftpIdentity =
    new RawKeySftpIdentity(privateKey, Some(privateKeyFilePassphrase), Some(publicKey))

  /**
   * Create SFTP identity for authenticating using private/public key file
   *
   * @param privateKey private key file to use when connecting
   */
  def createFileSftpIdentity(privateKey: String): KeyFileSftpIdentity =
    new KeyFileSftpIdentity(privateKey)

  /**
   * Create SFTP identity for authenticating using private/public key file
   *
   * @param privateKey private key file to use when connecting
   * @param privateKeyFilePassphrase password to use to decrypt private key file
   */
  def createFileSftpIdentity(privateKey: String, privateKeyFilePassphrase: Array[Byte]): KeyFileSftpIdentity =
    new KeyFileSftpIdentity(privateKey, Some(privateKeyFilePassphrase))
}

/**
 * SFTP identity for authenticating using private/public key value
 *
 * @param privateKey private key value to use when connecting
 * @param privateKeyFilePassphrase password to use to decrypt private key
 * @param publicKey public key value to use when connecting
 */
final class RawKeySftpIdentity @InternalApi private[ftp] (
    val privateKey: Array[Byte],
    val privateKeyFilePassphrase: Option[Array[Byte]] = None,
    val publicKey: Option[Array[Byte]] = None
) extends SftpIdentity {
  type KeyType = Array[Byte]

  def withPrivateKey(value: KeyType): RawKeySftpIdentity = copy(privateKey = value)

  def withPrivateKeyFilePassphrase(privateKeyFilePassphrase: Array[Byte]): RawKeySftpIdentity =
    copy(privateKeyFilePassphrase = Some(privateKeyFilePassphrase))

  def withPublicKey(publicKey: KeyType): RawKeySftpIdentity =
    copy(publicKey = Some(publicKey))

  private def copy(
      privateKey: Array[Byte] = privateKey,
      privateKeyFilePassphrase: Option[Array[Byte]] = privateKeyFilePassphrase,
      publicKey: Option[Array[Byte]] = publicKey
  ): RawKeySftpIdentity = new RawKeySftpIdentity(
    privateKey,
    privateKeyFilePassphrase,
    publicKey
  )

  override def toString: String =
    "RawKeySftpIdentity(" +
    s"privateKey(length)=${privateKey.length}," +
    s"privateKeyFilePassphrase.isDefined=${privateKeyFilePassphrase.isDefined}," +
    s"publicKey=${publicKey.isDefined})"
}

/**
 * SFTP identity for authenticating using private/public key file
 *
 * @param privateKey private key file to use when connecting
 * @param privateKeyFilePassphrase password to use to decrypt private key file
 */
final class KeyFileSftpIdentity @InternalApi private[ftp] (
    val privateKey: String,
    val privateKeyFilePassphrase: Option[Array[Byte]] = None
) extends SftpIdentity {
  type KeyType = String

  def withPrivateKey(value: String): KeyFileSftpIdentity = copy(privateKey = value)

  def withPrivateKeyFilePassphrase(privateKeyFilePassphrase: Array[Byte]): KeyFileSftpIdentity =
    copy(privateKeyFilePassphrase = Some(privateKeyFilePassphrase))

  private def copy(
      privateKey: String = privateKey,
      privateKeyFilePassphrase: Option[Array[Byte]] = privateKeyFilePassphrase
  ): KeyFileSftpIdentity = new KeyFileSftpIdentity(
    privateKey,
    privateKeyFilePassphrase
  )

}
