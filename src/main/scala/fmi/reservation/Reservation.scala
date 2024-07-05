package fmi.reservation

import cats.effect.IO
import fmi.club.{CourtAvailabilityAdjustment, CourtId}
import fmi.user.UserId
import fmi.utils.CirceUtils
import io.circe.Codec
import sttp.tapir.{Schema, SchemaType,CodecFormat}
import sttp.tapir
import java.time.Instant
import java.util.UUID

case class Reservation(
  reservationId: ReservationId,
  user: UserId,
  court: CourtId,
  startTime: Instant,
  placingTimestamp: Instant
) derives Codec,
      Schema
case class ReservationId(id: String)

object ReservationId:
  def generate: IO[ReservationId] = IO.randomUUID.map(uuid => ReservationId.apply(uuid.toString))

  given Codec[ReservationId] = CirceUtils.unwrappedCodec(ReservationId.apply)(_.id)
  given Schema[ReservationId] = Schema(SchemaType.SString())
  given tapir.Codec[String, ReservationId, CodecFormat.TextPlain] = tapir.Codec.string.map(ReservationId.apply)(_.id)