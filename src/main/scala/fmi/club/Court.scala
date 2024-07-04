package fmi.club

import doobie.Meta
import fmi.utils.CirceUtils
import io.circe.Codec
import io.circe.derivation.ConfiguredEnumCodec
import sttp.tapir
import sttp.tapir.{CodecFormat, Schema, SchemaType}
import fmi.utils.DerivationConfiguration.given_Configuration

case class CourtId(id: String)

object CourtId:
  given Codec[CourtId] = CirceUtils.unwrappedCodec(CourtId.apply)(_.id)
  given Schema[CourtId] = Schema(SchemaType.SString())
  given tapir.Codec[String, CourtId, CodecFormat.TextPlain] = tapir.Codec.string.map(CourtId.apply)(_.id)

case class Court(id: CourtId, name: String, surface: Surface, clubId: String) derives Codec, Schema

enum Surface derives ConfiguredEnumCodec:
  case Clay, Grass, Hard
object Surface:
  given Meta[Surface] = Meta[String].imap[Surface](Surface.valueOf)(_.toString)
  given Schema[Surface] = Schema.derivedEnumeration()
