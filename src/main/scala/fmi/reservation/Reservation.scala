package fmi.reservation

import cats.effect.IO
import doobie.Meta
import fmi.court.CourtId
import fmi.user.UserId
import fmi.utils.CirceUtils
import io.circe.Codec
import io.circe.derivation.ConfiguredEnumCodec
import sttp.tapir.{CodecFormat, Schema, SchemaType}
import sttp.tapir

import java.time.Instant
import fmi.utils.DerivationConfiguration.given_Configuration

case class Reservation(
  reservationId: ReservationId,
  user: UserId,
  court: CourtId,
  startTime: Instant,
  placingTimestamp: Instant,
  reservationStatus: ReservationStatus
) derives Codec,
      Schema
case class ReservationId(id: String)

object ReservationId:
  def generate: IO[ReservationId] = IO.randomUUID.map(uuid => ReservationId.apply(uuid.toString))

  given Codec[ReservationId] = CirceUtils.unwrappedCodec(ReservationId.apply)(_.id)
  given Schema[ReservationId] = Schema(SchemaType.SString())
  given tapir.Codec[String, ReservationId, CodecFormat.TextPlain] = tapir.Codec.string.map(ReservationId.apply)(_.id)

enum ReservationStatus derives ConfiguredEnumCodec:
  case Cancelled, Approved, Placed

object ReservationStatus:
  given Meta[ReservationStatus] =
    Meta[String].imap[ReservationStatus](x => ReservationStatus.valueOf(x.capitalize))(_.toString.toLowerCase)

  given Schema[ReservationStatus] = Schema.derivedEnumeration()
