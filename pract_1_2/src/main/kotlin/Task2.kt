import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.security.MessageDigest
import kotlin.system.measureTimeMillis

// SHA-256 содержимого файла, читаем кусками по 8 КБ
suspend fun sha256(file: File): String = withContext(Dispatchers.IO) {
    val digest = MessageDigest.getInstance("SHA-256")
    // Имитация долгого чтения большого файла: чем больше файл, тем дольше
    delay(100 + file.length() / 10)
    file.inputStream().use { input ->
        val buffer = ByteArray(8192)
        while (true) {
            ensureActive() // проверка отмены (таймаут)
            val read = input.read(buffer)
            if (read == -1) break
            digest.update(buffer, 0, read)
        }
    }
    digest.digest().joinToString("") { "%02x".format(it) }
}

fun findJsonFiles(dir: File): List<File> =
    dir.walkTopDown().filter { it.isFile && it.extension.equals("json", ignoreCase = true) }.toList()

fun main(args: Array<String>) = runBlocking {
    val dir = File(args.getOrNull(0) ?: "test_dir")
    val timeoutMs = args.getOrNull(1)?.toLongOrNull() ?: 5000L

    if (!dir.isDirectory) {
        println("Директория не найдена: ${dir.path}")
        return@runBlocking
    }

    val files = findJsonFiles(dir)
    println("Директория: ${dir.path}")
    println("Найдено json-файлов: ${files.size}, таймаут: $timeoutMs мс")
    println()

    var groups: Map<String, List<File>>? = null
    val time = measureTimeMillis {
        groups = withTimeoutOrNull(timeoutMs) {
            coroutineScope {
                // Для каждого файла своя корутина, хэши считаются параллельно
                val hashes = files.map { file ->
                    async {
                        val hash = sha256(file)
                        println("  ${hash.take(12)}...  ${file.relativeTo(dir).path}")
                        file to hash
                    }
                }.awaitAll()
                hashes.groupBy({ it.second }, { it.first }).filterValues { it.size > 1 }
            }
        }
    }

    println()
    val result = groups
    if (result == null) {
        // withTimeoutOrNull уже отменил все дочерние корутины
        println("Поиск прерван по таймауту")
    } else if (result.isEmpty()) {
        println("Дубликатов не найдено")
    } else {
        println("Найдено групп дубликатов: ${result.size}")
        var n = 1
        for ((hash, sameFiles) in result) {
            println("Группа ${n++} (SHA-256 ${hash.take(16)}...):")
            sameFiles.forEach { println("  - ${it.relativeTo(dir).path} (${it.length()} байт)") }
        }
    }
    println("Время: $time мс")
}
