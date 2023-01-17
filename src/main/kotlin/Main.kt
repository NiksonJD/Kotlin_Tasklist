package tasklist

import kotlinx.datetime.*
import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

data class Task(var priority: String, var date: String, var time: String, var description: String)

var tasks = mutableListOf<Task>()
fun input(prompt: String) = println(prompt).let { readln().trim().lowercase() }
fun format(answer: String) = answer.replace("\\t".toRegex(), " ").trim()

fun dueTag(date: String, time: String): String {
    val instant2 = Clock.System.now().toLocalDateTime(TimeZone.UTC).toInstant(TimeZone.UTC)
    val numberOfDays = Instant.parse("${date}T${time}:00Z").until(instant2, DateTimeUnit.DAY, TimeZone.UTC).toInt()
    return if (numberOfDays == 0) "T"
    else if (numberOfDays > 0) "O"
    else "I"
}

fun color(ch: String) = run {
    when (ch) {
        "C", "O" -> "\u001B[101m \u001B[0m"
        "T", "H" -> "\u001B[103m \u001B[0m"
        "N", "I" -> "\u001B[102m \u001B[0m"
        "L" -> "\u001B[104m \u001B[0m"
        else -> "\u001B[0m"
    }
}

fun printTask(): Int {
    if (tasks.isEmpty()) println("No tasks have been input") else
        println(
            """
                +----+------------+-------+---+---+--------------------------------------------+
                | N  |    Date    | Time  | P | D |                   Task                     |
                +----+------------+-------+---+---+--------------------------------------------+
                """.trimIndent()
        )
    for ((i, it) in tasks.withIndex()) {
        val l =
            listOf(i.inc().toString().padEnd(3), it.date, it.time, color(it.priority), color(dueTag(it.date, it.time)))
        it.description.lines().forEachIndexed { i2, it2 ->
            it2.chunked(44).forEachIndexed { i3, it3 ->
                if (i2 == 0 && i3 == 0) print("| ${l[0]}| ${l[1]} | ${l[2]} | ${l[3]} | ${l[4]} |")
                else print("|    |            |       |   |   |")
                println(it3.padEnd(44) + "|")
            }
        }
        println("+----+------------+-------+---+---+--------------------------------------------+")
    }
    return tasks.size
}

fun tasklistJson(save: Boolean) {
    val jsonFile = File("tasklist.json")
    if (save || jsonFile.exists()) {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val type = Types.newParameterizedType(MutableList::class.java, Task::class.java)
        val taskListAdapter = moshi.adapter<MutableList<Task>>(type)
        if (save) jsonFile.writeText(taskListAdapter.toJson(tasks)) else
            tasks = taskListAdapter.fromJson(jsonFile.readText()) as (MutableList<Task>)
    }
}

fun userInput(aw: Int): String {
    var answer = ""
    when (aw) {
        0 -> do {
            answer = println("Input the task priority (C, H, N, L):").let { readln().uppercase() }
        } while ("CHNL".indexOf(answer) == -1 || answer.isEmpty())

        1 -> do {
            println("Input the date (yyyy-mm-dd):")
            answer = try {
                readln().split("-").map { it.toInt() }.let { LocalDate(it[0], it[1], it[2]).toString() }
            } catch (e: Exception) {
                "".also { println("The input date is invalid") }
            }
        } while (answer.isEmpty())

        2 -> do {
            val answer2 = input("Input the time (hh:mm):")
            if (answer2.matches(Regex("(0?[0-9]|1[0-9]|2[0-3]):?[0-5]?[0-9]"))) {
                answer = answer2.split(":").let { it[0].padStart(2, '0') + ":" + it[1].padStart(2, '0') }
            } else println("The input time is invalid")
        } while (answer.isEmpty())

        3 -> {
            println("Input a new task (enter a blank line to end):")
            while (true) {
                val ans = format(readln())
                if (ans.isEmpty()) break
                answer += "$ans\n"
            }
        }
    }
    return answer
}

fun taskList() {
    tasklistJson(false)
    while (true) {
        when (val resp = format(input("Input an action (add, print, edit, delete, end):"))) {
            "end" -> break
            "add" -> {
                val aw = listOf(userInput(0), userInput(1), userInput(2), userInput(3))
                if (aw[3].isNotEmpty()) tasks.add(Task(aw[0], aw[1], aw[2], aw[3])) else println("The task is blank")
            }

            "print" -> printTask()
            "delete", "edit" -> {
                val size = printTask()
                if (size > 0) {
                    var aw4 = ""
                    while (true) {
                        println("Input the task number (1-${size}):").let { aw4 = format(readln()) }
                        if (aw4.first()
                                .isLetter() || aw4.toInt() < 0 || aw4.toInt() !in 1..size
                        ) println("Invalid task number") else {
                            val index = aw4.toInt().dec()
                            if (resp == "delete") tasks.removeAt(index).also { println("The task is deleted") }
                            if (resp == "edit") {
                                do {
                                    val re = input("Input a field to edit (priority, date, time, task):")
                                    when (re) {
                                        "priority" -> tasks[index].priority = userInput(0)
                                        "date" -> tasks[index].date = userInput(1)
                                        "time" -> tasks[index].time = userInput(2)
                                        "task" -> tasks[index].description = userInput(3)
                                        else -> println("Invalid field")
                                    }
                                } while (re !in listOf("priority", "date", "time", "task"))
                                println("The task is changed")
                            }
                            break
                        }
                    }
                }
            }

            else -> println("The input action is invalid")
        }
    }
    tasklistJson(true)
}

fun main() = taskList().also { println("Tasklist exiting!") }