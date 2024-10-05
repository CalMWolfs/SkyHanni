package at.skyhanni.sharedvariables.buildprocess

// import at.hannibal2.changelog.SkyHanniChangelogBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

abstract class ChangelogVerification : DefaultTask() {

    @Input
    var prTitle: String = ""

    @Input
    var prBody: String = ""

    @get:Internal
    val prBodyLines get() = prBody.lines()

    private val prLink = "https://example.com/pr/1"

    @TaskAction
    fun scanChangelog() {
//         val (changes, bodyErrors) = SkyHanniChangelogBuilder.findChanges(prBodyLines, prLink)
//         val titleErrors = SkyHanniChangelogBuilder.findPullRequestNameErrors(prTitle, changes)
//
//         // todo do something with the errors
//         if (bodyErrors.isEmpty() && titleErrors.isEmpty()) {
//             println("Changelog verification successful")
//         } else {
//             println("Changelog verification failed")
//             bodyErrors.forEach { println(it.message) }
//             titleErrors.forEach { println(it.message) }
//         }
        println("prTitle: $prTitle")
        println("prBody: $prBody")
    }
}
