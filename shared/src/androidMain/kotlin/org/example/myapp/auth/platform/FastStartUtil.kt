package org.example.myapp.auth.platform

import java.nio.ByteBuffer
import java.nio.ByteOrder

actual object FastStartUtil {
    private data class Atom(val type: String, val offset: Int, val size: Int)

    actual fun process(inputBytes: ByteArray): ByteArray {
        if (inputBytes.size < 32) return inputBytes

        return try {
            val atoms = parseRootAtoms(inputBytes)
            val moov = atoms.find { it.type == "moov" } ?: return inputBytes
            val mdat = atoms.find { it.type == "mdat" } ?: return inputBytes

            // 1. 이미 moov가 mdat보다 앞에 있으면 변환 불필요 (원본 반환)
            if (moov.offset < mdat.offset) {
                return inputBytes
            }

            // 2. moov atom 복사 및 오프셋 보정 (stco, co64)
            val moovBytes = inputBytes.copyOfRange(moov.offset, moov.offset + moov.size)
            patchChunkOffsets(moovBytes, moov.size.toLong())

            // 3. FastStart 순서로 새 바이트 배열 조립: [ftyp 등] + [moov] + [mdat 본문] + [잔여 atom]
            val result = ByteArray(inputBytes.size)
            var currentPos = 0

            // [Step A] mdat 이전 atom들 (주로 ftyp)
            if (mdat.offset > 0) {
                System.arraycopy(inputBytes, 0, result, currentPos, mdat.offset)
                currentPos += mdat.offset
            }

            // [Step B] 앞으로 당겨온 moov 삽입
            System.arraycopy(moovBytes, 0, result, currentPos, moov.size)
            currentPos += moov.size

            // [Step C] mdat부터 moov 직전까지 (영상/음성 본문)
            val mdatToMoovLength = moov.offset - mdat.offset
            System.arraycopy(inputBytes, mdat.offset, result, currentPos, mdatToMoovLength)
            currentPos += mdatToMoovLength

            // [Step D] moov 이후 잔여 데이터
            val remaining = inputBytes.size - (moov.offset + moov.size)
            if (remaining > 0) {
                System.arraycopy(inputBytes, moov.offset + moov.size, result, currentPos, remaining)
            }

            result
        } catch (e: Exception) {
            inputBytes // 예외 발생 시 원본으로 안전하게 폴백
        }
    }

    private fun parseRootAtoms(bytes: ByteArray): List<Atom> {
        val atoms = mutableListOf<Atom>()
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        var pos = 0

        while (pos + 8 <= bytes.size) {
            buffer.position(pos)
            val size32 = buffer.int.toLong() and 0xFFFFFFFFL
            val typeBytes = ByteArray(4)
            buffer.get(typeBytes)
            val type = String(typeBytes, Charsets.US_ASCII)

            val size = when (size32) {
                1L -> {
                    if (pos + 16 > bytes.size) break
                    buffer.long.toInt()
                }
                0L -> bytes.size - pos
                else -> size32.toInt()
            }

            if (size <= 0 || pos + size > bytes.size) break
            atoms.add(Atom(type, pos, size))
            pos += size
        }
        return atoms
    }

    private fun patchChunkOffsets(moovBytes: ByteArray, shiftAmount: Long) {
        val buffer = ByteBuffer.wrap(moovBytes).order(ByteOrder.BIG_ENDIAN)
        val stcoTag = 0x7374636F // "stco"
        val co64Tag = 0x636F3634 // "co64"

        for (i in 0 until moovBytes.size - 8) {
            val tag = buffer.getInt(i)
            if (tag == stcoTag) {
                val entryCount = buffer.getInt(i + 8)
                val entriesStart = i + 12
                for (e in 0 until entryCount) {
                    val pos = entriesStart + e * 4
                    if (pos + 4 <= moovBytes.size) {
                        val currentOffset = buffer.getInt(pos).toLong() and 0xFFFFFFFFL
                        buffer.putInt(pos, (currentOffset + shiftAmount).toInt())
                    }
                }
            } else if (tag == co64Tag) {
                val entryCount = buffer.getInt(i + 8)
                val entriesStart = i + 12
                for (e in 0 until entryCount) {
                    val pos = entriesStart + e * 8
                    if (pos + 8 <= moovBytes.size) {
                        val currentOffset = buffer.getLong(pos)
                        buffer.putLong(pos, currentOffset + shiftAmount)
                    }
                }
            }
        }
    }
}