package fmi.court

import doobie.Meta
import fmi.club.ClubId
import fmi.user.UserId
import fmi.utils.CirceUtils
import io.circe.Codec
import io.circe.derivation.ConfiguredEnumCodec
import sttp.tapir
import sttp.tapir.{CodecFormat, Schema, SchemaType}
import fmi.utils.DerivationConfiguration.given_Configuration

import java.util.UUID

case class CourtId(id: String)
object CourtId:
  given Codec[CourtId] = CirceUtils.unwrappedCodec(CourtId.apply)(_.id)
  given Schema[CourtId] = Schema(SchemaType.SString())
  given tapir.Codec[String, CourtId, CodecFormat.TextPlain] = tapir.Codec.string.map(CourtId.apply)(_.id)


case class Court(id: CourtId, name: String, surface: Surface, clubId: ClubId) derives Codec, Schema
object Court:
  def fromDto(dto: CourtDto, clubId: ClubId): Court = Court(dto.id.getOrElse(UUID.randomUUID().toCourtId), dto.name, dto.surface, clubId)
  extension (uuid: UUID)
    def toCourtId: CourtId = CourtId(uuid.toString)


case class CourtDto(id: Option[CourtId], name: String, surface: Surface) derives Codec, Schema

enum Surface derives ConfiguredEnumCodec:
  case Clay, Grass, Hard
object Surface:
  given Meta[Surface] = Meta[String].imap[Surface](s => Surface.valueOf(s.capitalize))(_.toString)
  given Schema[Surface] = Schema.derivedEnumeration()
