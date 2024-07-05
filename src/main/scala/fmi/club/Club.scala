package fmi.club

import fmi.user.UserId
import fmi.utils.CirceUtils
import io.circe.Codec
import sttp.tapir
import sttp.tapir.{CodecFormat, Schema, SchemaType}

import java.util.UUID

case class ClubId(id: String)
object ClubId:
  extension (id: String) def toClubId: ClubId = ClubId(id)
  given Codec[ClubId] = CirceUtils.unwrappedCodec(ClubId.apply)(_.id)
  given Schema[ClubId] = Schema(SchemaType.SString())
  given tapir.Codec[String, ClubId, CodecFormat.TextPlain] = tapir.Codec.string.map(ClubId.apply)(_.id)

case class Club(id: ClubId, name: String, description: String, owner: UserId) derives Codec, Schema
object Club:
  def fromDto(owner: UserId, dto: ClubDto): Club = Club(UUID.randomUUID().toClubId, dto.name, dto.description, owner)
  extension (uuid: java.util.UUID) def toClubId: ClubId = ClubId(uuid.toString)

case class ClubDto(name: String, description: String, owner: UserId) derives Codec, Schema
