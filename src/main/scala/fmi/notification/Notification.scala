package fmi.notification

import cats.effect.IO
import fmi.court.CourtId
import fmi.user.UserId
import fmi.club.ClubId
import fmi.reservation.ReservationId
import java.time.Instant
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*
import doobie.Meta
import io.circe.derivation.ConfiguredCodec
import sttp.tapir.{CodecFormat, Schema, SchemaType}
import sttp.tapir
import io.circe.Codec
import fmi.utils.DerivationConfiguration.given_Configuration
import fmi.utils.CirceUtils

case class Notification(
  id: NotificationId,
  notificationType: NotificationType,
  triggeredBy: UserId,
  clubId: Option[ClubId],
  courtId: Option[CourtId],
  reservationId: Option[ReservationId],
  status: NotificationStatus,
  createdAt: Instant,
  targetUser: Option[UserId]
) derives Codec,
      Schema

case class NotificationId(id: String)

object NotificationId:
  def generate: IO[NotificationId] = IO.randomUUID.map(uuid => NotificationId.apply(uuid.toString))

  given Codec[NotificationId] = CirceUtils.unwrappedCodec(NotificationId.apply)(_.id)

  given Schema[NotificationId] = Schema(SchemaType.SString())

  given tapir.Codec[String, NotificationId, CodecFormat.TextPlain] = tapir.Codec.string.map(NotificationId.apply)(_.id)

enum NotificationType derives ConfiguredCodec:
  case ClubCreationRequest, CourtCreationRequest, ReservationCreationRequest, ReservationCancelled,
    ClubCreationRequestDenied, CourtCreationRequestDenied, ReservationDeleted, ReservationApproved, ClubCreated,
    CourtCreated, CourtUpdated, ClubTransferred

object NotificationType:
  private def fromString(s: String): Option[NotificationType] = s.toLowerCase match
    case "club_creation_request" => Some(ClubCreationRequest)
    case "court_creation_request" => Some(CourtCreationRequest)
    case "reservation_creation_request" => Some(ReservationCreationRequest)
    case "reservation_cancelled" => Some(ReservationCancelled)
    case "club_creation_request_denied" => Some(ClubCreationRequestDenied)
    case "court_creation_request_denied" => Some(CourtCreationRequestDenied)
    case "reservation_deleted" => Some(ReservationDeleted)
    case "reservation_approved" => Some(ReservationApproved)
    case "club_created" => Some(ClubCreated)
    case "court_created" => Some(CourtCreated)
    case "court_updated" => Some(CourtUpdated)
    case "club_transferred" => Some(ClubTransferred)
    case _ => None

  private def toString(notificationType: NotificationType): String = notificationType match
    case ReservationDeleted => "reservation_deleted"
    case ClubCreationRequest => "club_creation_request"
    case CourtCreationRequest => "court_creation_request"
    case ReservationCreationRequest => "reservation_creation_request"
    case ReservationCancelled => "reservation_cancelled"
    case ClubCreationRequestDenied => "club_creation_request_denied"
    case CourtCreationRequestDenied => "court_creation_request_denied"
    case ReservationApproved => "reservation_approved"
    case ClubCreated => "club_created"
    case CourtCreated => "court_created"
    case ClubTransferred => "club_transferred"
    case CourtUpdated => "court_updated"

  given Meta[NotificationType] =
    Meta[String].imap[NotificationType](x => NotificationType.fromString(x).get)(x => toString(x))

  given Schema[NotificationType] = Schema.derivedEnumeration()

enum NotificationStatus derives ConfiguredCodec:
  case Read, NotRead

object NotificationStatus:
  private def fromString(s: String): Option[NotificationStatus] = s.toLowerCase match
    case "read" => Some(NotificationStatus.Read)
    case "not_read" => Some(NotificationStatus.NotRead)
    case _ => None

  private def toString(notificationStatus: NotificationStatus): String = notificationStatus match
    case NotRead => "not_read"
    case Read => "read"

  given Meta[NotificationStatus] =
    Meta[String].imap[NotificationStatus](x => NotificationStatus.fromString(x).get)(x => toString(x))

  given Schema[NotificationStatus] = Schema.derivedEnumeration()
