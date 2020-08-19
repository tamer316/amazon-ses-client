package dev.choppers.ses

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.Properties
import java.util.concurrent.{ExecutorService, Future => JFuture}

import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.auth._
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.regions.Region
import com.amazonaws.services.simpleemail._
import com.amazonaws.services.simpleemail.model._
import dev.choppers.ses.model.Email
import javax.activation.DataHandler

import scala.collection.JavaConverters._
import javax.mail.Message.RecipientType
import javax.mail.internet.{MimeBodyPart, MimeMessage, MimeMultipart}
import javax.mail.util.ByteArrayDataSource
import javax.mail.{Address, Session}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

trait SES {
  self: SESClient =>

  import aws._
  import dev.choppers.ses.model.Address.toInternetAddress

  def buildRawRequest(email: Email): SendRawEmailRequest = {
    val props = new Properties()

    email.returnPath.foreach(props.put("mail.smtp.from", _))

    val session = Session.getDefaultInstance(props)

    // Create a new MimeMessage object.
    val message = new MimeMessage(session)

    // Add subject, from and to lines.
    message.setSubject(email.subject.data, email.subject.charset)

    addAddresses(message, email)

    // Create a multipart/mixed parent container.
    val msg = new MimeMultipart("mixed")

    // Add the parent container to the message.
    message.setContent(msg)

    // Create a wrapper for the HTML and text parts.
    val wrap = new MimeBodyPart()

    addBody(wrap, email)

    // Add the multipart/alternative part to the message.
    msg.addBodyPart(wrap)

    addAttachments(msg, email)

    // Send the email.
    val outputStream = new ByteArrayOutputStream()
    message.writeTo(outputStream)
    val rawMessage = new RawMessage(ByteBuffer.wrap(outputStream.toByteArray))

    new SendRawEmailRequest(rawMessage)
  }

  def buildRequest(email: Email): SendEmailRequest = {
    val destination = new Destination()
    if (email.to.nonEmpty) destination.setToAddresses(email.to.map(_.encoded).asJavaCollection)
    if (email.cc.nonEmpty) destination.setCcAddresses(email.cc.map(_.encoded).asJavaCollection)
    if (email.bcc.nonEmpty) destination.setBccAddresses(email.bcc.map(_.encoded).asJavaCollection)

    val subject = new Content(email.subject.data).withCharset(email.subject.charset)

    val body = new Body()
    email.bodyHtml.foreach { bodyHtml =>
      val htmlContent = new Content(bodyHtml.data)
      htmlContent.setCharset(bodyHtml.charset)
      body.setHtml(htmlContent)
    }
    email.bodyText.foreach { bodyText =>
      val textContent = new Content(bodyText.data)
      textContent.setCharset(bodyText.charset)
      body.setText(textContent)
    }

    val message = new Message(subject, body)

    val req = new SendEmailRequest(email.source.encoded, destination, message)
    if (email.replyTo.nonEmpty) req.setReplyToAddresses(email.replyTo.map(_.encoded).asJavaCollection)

    email.returnPath.foreach(req.setReturnPath)

    req
  }

  def send(email: Email): Future[Unit] =
    (if (email.attachments.isEmpty) {
      wrapAsyncMethod {
        sendEmailAsync(buildRequest(email), _: AsyncHandler[SendEmailRequest, SendEmailResult])
      }
    } else {
      wrapAsyncMethod {
        sendRawEmailAsync(buildRawRequest(email), _: AsyncHandler[SendRawEmailRequest, SendRawEmailResult])
      }
    }).map(_ => ())

  private def addBody(wrap: MimeBodyPart, email: Email): Unit = {
    // Create a multipart/alternative child container.
    val msgBody = new MimeMultipart("alternative")

    email.bodyText.foreach { bodyText =>
      val textPart = new MimeBodyPart()
      textPart.setContent(bodyText.data, s"text/plain; charset=${bodyText.charset}")
      msgBody.addBodyPart(textPart)
    }

    email.bodyHtml.foreach { bodyHtml =>
      val htmlPart = new MimeBodyPart()
      htmlPart.setContent(bodyHtml.data, s"text/html; charset=${bodyHtml.charset}")
      msgBody.addBodyPart(htmlPart)
    }

    // Add the child container to the wrapper object.
    wrap.setContent(msgBody)
  }

  private def addAddresses(message: MimeMessage, email: Email): Unit = {
    message.setFrom(email.source)

    if (email.to.nonEmpty) {
      message.setRecipients(RecipientType.TO, email.to.map(toInternetAddress).toArray[Address])
    }

    if (email.cc.nonEmpty) {
      message.setRecipients(RecipientType.CC, email.cc.map(toInternetAddress).toArray[Address])
    }

    if (email.bcc.nonEmpty) {
      message.setRecipients(RecipientType.BCC, email.bcc.map(toInternetAddress).toArray[Address])
    }

    if (email.replyTo.nonEmpty) {
      message.setReplyTo(email.replyTo.map(toInternetAddress).toArray[Address])
    }
  }

  private def addAttachments(msg: MimeMultipart, email: Email): Unit = {
    email.attachments.foreach { attachment =>
      // Define the attachment
      val att = new MimeBodyPart()
      val fds = new ByteArrayDataSource(attachment.contents, attachment.contentType)
      att.setDataHandler(new DataHandler(fds))
      att.setFileName(attachment.name)

      // Add the attachment to the message.
      msg.addBodyPart(att)
    }
  }

  /**
    * Convert result to scala.concurrent.Future from java.util.concurrent.Future.
    */
  private def wrapAsyncMethod[Request <: AmazonWebServiceRequest, Result]
  (execute: AsyncHandler[Request, Result] => JFuture[Result]): Future[Result] = {
    val p = Promise[Result]()
    execute {
      new AsyncHandler[Request, Result] {
        def onError(exception: Exception): Unit = p.failure(exception)

        def onSuccess(request: Request, result: Result): Unit = p.success(result)
      }
    }
    p.future
  }
}

object SESClient {

  def apply(accessKeyId: String, secretKeyId: String)(implicit region: Region): SESClient = {
    apply(new BasicAWSCredentials(accessKeyId, secretKeyId))
  }

  def apply(awsCredentials: AWSCredentials = new AnonymousAWSCredentials)(implicit region: Region): SESClient = {
    val client = new AmazonSimpleEmailServiceAsyncClient(awsCredentials)
    client.setRegion(region)
    new SESClient(client)
  }

  def apply(awsCredentials: AWSCredentials, executorService: ExecutorService)(implicit region: Region): SESClient = {
    val client = new AmazonSimpleEmailServiceAsyncClient(awsCredentials, executorService)
    client.setRegion(region)
    new SESClient(client)
  }

  def apply(awsCredentialsProvider: AWSCredentialsProvider)(implicit region: Region): SESClient = {
    val client = new AmazonSimpleEmailServiceAsyncClient(awsCredentialsProvider)
    client.setRegion(region)
    new SESClient(client)
  }

  def apply(awsCredentialsProvider: AWSCredentialsProvider, executorService: ExecutorService)
           (implicit region: Region): SESClient = {
    val client = new AmazonSimpleEmailServiceAsyncClient(awsCredentialsProvider, executorService)
    client.setRegion(region)
    new SESClient(client)
  }
}

class SESClient(val aws: AmazonSimpleEmailServiceAsync) extends SES
