/*
 * Copyright (C) 2016-2019 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.stream.alpakka.hdfs.impl.writer

import akka.annotation.InternalApi
import akka.stream.alpakka.hdfs.FilePathGenerator
import akka.stream.alpakka.hdfs.impl.writer.HdfsWriter._
import akka.util.ByteString
import org.apache.hadoop.fs.{FSDataOutputStream, FileSystem, Path}

/**
 * Internal API
 */
@InternalApi
private[writer] final case class DataWriter(
    fs: FileSystem,
    pathGenerator: FilePathGenerator,
    maybeTargetPath: Option[Path],
    overwrite: Boolean
) extends HdfsWriter[FSDataOutputStream, ByteString] {

  protected lazy val target: Path =
    getOrCreatePath(maybeTargetPath, createTargetPath(pathGenerator, 0))

  def sync(): Unit = output.hsync()

  def write(input: ByteString, separator: Option[Array[Byte]]): Long = {
    val bytes = input.toArray
    output.write(bytes)
    separator.foreach(output.write)
    output.size()
  }

  def rotate(rotationCount: Long): DataWriter = {
    output.close()
    copy(maybeTargetPath = Some(createTargetPath(pathGenerator, rotationCount)))
  }

  def create(fs: FileSystem, file: Path): FSDataOutputStream = fs.create(file, overwrite)

}

private[hdfs] object DataWriter {
  def apply(fs: FileSystem, pathGenerator: FilePathGenerator, overwrite: Boolean): DataWriter =
    new DataWriter(fs, pathGenerator, None, overwrite)
}
