package io.github.rusticflare.photoorganiser

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import java.io.File
import java.time.LocalDate
import java.time.format.TextStyle.FULL
import java.util.Date
import java.util.Locale.ENGLISH

class Organise : CliktCommand(help = "Copy SOURCE to DEST, or multiple SOURCE(s) to directory DEST.") {

    private val source by argument().file(exists = true, fileOkay = false)
    private val target by argument().file(fileOkay = false)

    override fun run() {
        source.walkTopDown().asSequence()
            .filter { it.isFile }
            .forEach { file -> file.moveInTo(target.getSubFolderFor(file.dateTaken)) }
    }

}

private fun File.moveInTo(destinationFolder: File) {
    val destinationFile = destinationFolder.resolve(name)
    if (destinationFile.doesNotExist()) {
        copyTo(destinationFile)
        delete()
    }
}

private fun File.doesNotExist() = !exists()

private fun File.getSubFolderFor(date: LocalDate): File {
    return resolve("${date.year}")
        .resolve("${date.monthValue.withTwoDigits()} - ${date.month.getDisplayName(FULL, ENGLISH)}")
        .resolve(date.dayOfMonth.withTwoDigits())
}

private fun Int.withTwoDigits() = when (this) {
    in 1..9 -> "0$this"
    else -> "$this"
}

private val File.dateTaken: LocalDate
    get() {
        return ImageMetadataReader.readMetadata(this)
            .getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)
            .getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)
            .toLocalDate()
    }

fun Date.toLocalDate(): LocalDate {
    return java.sql.Date(time).toLocalDate()
}

fun main(args: Array<String>) = Organise().main(args)
