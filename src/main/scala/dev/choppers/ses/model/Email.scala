package dev.choppers.ses.model

import javax.mail.internet.InternetAddress

case class Address(address: String,
                   personal: Option[String],
                   charset: String,
                   encoded: String)

object Address {
  def apply(address: String, personal: Option[String] = None, charset: String = "UTF-8"): Address = {
    val encoded = personal
      .map(new InternetAddress(address, _, charset))
      .getOrElse(new InternetAddress(address))
      .toString
    new Address(address, personal, charset, encoded)
  }

  implicit def toInternetAddress(address: Address): InternetAddress = {
    address.personal
      .map(new InternetAddress(address.address, _, address.charset))
      .getOrElse(new InternetAddress(address.address))
  }
}


case class Content(data: String,
                   charset: String = "UTF-8",
                   contentTransferEncoding: Option[String] = None)

case class EmailAttachment(contentType: String,
                           name: String,
                           contents: Array[Byte],
                           contentDisposition: Option[String] = None,
                           contentTransferEncoding: Option[String] = None)

case class Email(subject: Content,
                 source: Address,
                 bodyText: Option[Content] = None,
                 bodyHtml: Option[Content] = None,
                 to: Seq[Address] = Seq.empty,
                 cc: Seq[Address] = Seq.empty,
                 bcc: Seq[Address] = Seq.empty,
                 replyTo: Seq[Address] = Seq.empty,
                 returnPath: Option[String] = None,
                 attachments: Seq[EmailAttachment] = Seq.empty)
