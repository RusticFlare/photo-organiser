package io.github.rusticflare.photoorganiser

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle.FULL
import java.util.Date
import java.util.Locale.ENGLISH

fun main(args: Array<String>) = Organise().main(args)

class Organise : CliktCommand(help = "<WIP> Copy SOURCE to DEST, or multiple SOURCE(s) to directory DEST.") {

    private val source by argument().file(exists = true, fileOkay = false)
    private val target by argument().file(fileOkay = false)

    private val destinationFolders = mutableSetOf<File>()

    override fun run() {
        source.walk()
            .filter { it.isFile }
            .forEach { file -> file.copyToTargetFolder() }

        logDestinationFolders()
    }

    private fun logDestinationFolders() {
        println("Photos were imported to:")
        destinationFolders
            .map { it.absolutePath }
            .sorted()
            .forEach { println(it) }
    }

    private fun File.copyToTargetFolder() = try {
        copyInTo(target.getSubFolderFor(dateTaken))
    } catch (exception: Exception) {
        println("Copy failed <$name> : ${exception::class.simpleName} ${exception.message}")
    }

    private fun File.copyInTo(destinationFolder: File) {
        val destinationFile = destinationFolder.resolve(name)
        if (destinationFile.doesNotExist()) {
            destinationFolders.add(destinationFolder)
            copyTo(destinationFile)
        }
    }
}

private fun File.doesNotExist() = !exists()

private fun File.getSubFolderFor(date: LocalDate) =
    resolve("${date.year}")
        .resolve("${date.monthValue.toTwoDigitString()} - ${date.month.getDisplayName(FULL, ENGLISH)}")
        .resolve(date.dayOfMonth.toTwoDigitString())

private fun Int.toTwoDigitString() = when (this) {
    in 1..9 -> "0$this"
    in 10..31 -> "$this"
    else -> throw IllegalArgumentException("Number must be between 1 and 31 (inclusive)")
}

private val File.dateTaken: LocalDate
    get() = ImageMetadataReader.readMetadata(this)
        .getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)
        .dateOriginal
        .toLocalDate()

private fun Date.toLocalDate() = LocalDate.ofInstant(toInstant(), ZoneId.systemDefault())
