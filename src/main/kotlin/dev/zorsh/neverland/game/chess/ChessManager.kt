package dev.zorsh.neverland.game.chess

import dev.zorsh.neverland.game.util.font
import net.kyori.adventure.key.Key
import net.kyori.adventure.nbt.BinaryTag
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.dialog.*
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerCustomClickEvent
import java.util.*

class ChessManager {
    enum class ChessPiece(val textValue: Char) {
        PAWN('p'),
        KNIGHT('n'),
        BISHOP('b'),
        ROOK('r'),
        QUEEN('q'),
        KING('k')
    }

    data class ChessPlayerData(
        val piecesPlacement: MutableMap<Pair<Int, Int>, ChessPiece>,
        var selectedPiece: Pair<Int, Int>? = null,
    )

    companion object {
        val playerData = mutableMapOf<UUID, ChessPlayerData>()
        val CHESS_CLICK_PIECE = Key.key("chess", "piece_click")
        val CHESS_CLICK_CELL = Key.key("chess", "cell_click")
        val CHESS_EXIT = Key.key("chess", "exit")

        private fun getBasePlacement(): MutableMap<Pair<Int, Int>, ChessPiece> {
            val result = mutableMapOf<Pair<Int, Int>, ChessPiece>()

            for (i in 1..8) {
                result[i to 2] = ChessPiece.PAWN
            }
            result[1 to 1] = ChessPiece.ROOK
            result[2 to 1] = ChessPiece.KNIGHT
            result[3 to 1] = ChessPiece.BISHOP
            result[4 to 1] = ChessPiece.QUEEN
            result[5 to 1] = ChessPiece.KING
            result[6 to 1] = ChessPiece.BISHOP
            result[7 to 1] = ChessPiece.KNIGHT
            result[8 to 1] = ChessPiece.ROOK

            return result
        }

        private fun showChessBoard(player: Player) {
            val playerData = playerData[player.uuid] ?: return

            val dMeta =
                DialogMetadata(
                    Component.text("Шахматы"),
                    null,
                    true,
                    false,
                    DialogAfterAction.NONE,
                    listOf(
                        DialogBody.PlainMessage(Component.text(0).font("chess:main"), 1024)
                    ),
                    emptyList()
                )

            val chessButtons =
                List(64) { i ->
                    val x = 1 + i % 8
                    val y = 8 - (i / 8) % 8

                    val buttonPayload = CompoundBinaryTag.builder()
                        .putInt("x", x)
                        .putInt("y", y)
                        .build()

                    val piece = playerData.piecesPlacement[x to y]
                    if (piece != null) {
                        var pieceText = Component.text(piece.textValue).font("chess:pieces").color(TextColor.color(51, 156, 255))
                        if (playerData.selectedPiece == x to y) {
                            pieceText = Component.text(piece.textValue).font("chess:pieces").color(TextColor.color(247, 242, 82))
                        }

                        return@List DialogActionButton(
                            pieceText,
                            null,
                            20,
                            DialogAction.Custom(CHESS_CLICK_PIECE, buttonPayload)
                        )
                    }

                    return@List DialogActionButton(
                        Component.text(""),
                        null,
                        20,
                        DialogAction.Custom(CHESS_CLICK_CELL, buttonPayload)
                    )
                }

            val chessDialog =
                Dialog.MultiAction(
                    dMeta,
                    chessButtons,
                    DialogActionButton(
                        Component.text("Выход"),
                        Component.text("НЕ НАЖИМАЙ").color(NamedTextColor.RED),
                        80,
                        DialogAction.Custom(CHESS_EXIT, CompoundBinaryTag.empty()),
                    ),
                    8
                )
            player.showDialog(chessDialog)
        }

        private fun clickedBoardPiece(player: Player, x: Int, y: Int) {
            val data = playerData[player.uuid]
            if (data == null) {
                showChessBoard(player)
                return
            }

            if (playerData[player.uuid]?.selectedPiece == x to y) {
                playerData[player.uuid]?.selectedPiece = null
            } else {
                playerData[player.uuid]?.selectedPiece = x to y
            }
            showChessBoard(player)
        }

        private fun clickedBoardCell(player: Player, x: Int, y: Int) {
            val data = playerData[player.uuid]
            if (data == null) {
                showChessBoard(player)
                return
            }

            val selectedPiece = playerData[player.uuid]?.selectedPiece
            if (selectedPiece != null) {
                val piece = playerData[player.uuid]?.piecesPlacement?.get(selectedPiece)
                playerData[player.uuid]?.selectedPiece = null

                if (piece == null) {
                    showChessBoard(player)
                    return
                }

                playerData[player.uuid]?.piecesPlacement?.remove(selectedPiece)
                playerData[player.uuid]?.piecesPlacement[x to y] = piece
            }
            showChessBoard(player)
        }

        fun startChess(player: Player) {
            playerData[player.uuid] = ChessPlayerData(getBasePlacement())
            showChessBoard(player)
        }

        fun registerListener() {
            MinecraftServer.getGlobalEventHandler().addListener(PlayerCustomClickEvent::class.java) { event ->
                when (event.key) {
                    CHESS_CLICK_PIECE -> {
                        val payload = event.payload
                        if (payload is CompoundBinaryTag) {
                            val x = payload.getInt("x")
                            val y = payload.getInt("y")

                            clickedBoardPiece(event.player, x, y)
                        }
                    }
                    CHESS_CLICK_CELL -> {
                        val payload = event.payload
                        if (payload is CompoundBinaryTag) {
                            val x = payload.getInt("x")
                            val y = payload.getInt("y")

                            clickedBoardCell(event.player, x, y)
                        }
                    }
                    CHESS_EXIT -> {
                        event.player.closeDialog()
                    }
                }
            }
        }
    }
}