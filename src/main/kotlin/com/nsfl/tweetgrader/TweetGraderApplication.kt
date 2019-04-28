package com.nsfl.tweetgrader

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

private const val TWEETS_HTML = "<!doctypehtml><link href=https://cdn.datatables.net/1.10.19/css/jquery.dataTables.min.css rel=stylesheet><link href=https://cdn.datatables.net/select/1.3.0/css/select.dataTables.min.css rel=stylesheet><link href=https://cdn.datatables.net/buttons/1.5.6/css/buttons.dataTables.min.css rel=stylesheet><script src=https://code.jquery.com/jquery-3.3.1.js></script><script src=https://cdn.datatables.net/1.10.19/js/jquery.dataTables.min.js></script><script src=https://cdn.datatables.net/select/1.3.0/js/dataTables.select.min.js></script><script src=https://cdn.datatables.net/buttons/1.5.6/js/dataTables.buttons.min.js></script><script class=init>var dataSet = [%s]; \$(document).ready(function() { var table = \$('#table').DataTable({ paging: false, order: [ [0, \"asc\"] ], data: dataSet, columns: [ { title: 'Username' }, { title: 'Content' }, { title: 'Likes' }, { title: 'Retweets' }, { title: '' }], columnDefs: [ { orderable: false, className: 'select-checkbox', targets: 4 } ], select: { style: 'multi' }, dom: 'Bfrtip', buttons: [ { text: 'Generate Payouts', action: function () { var selected = table.rows( { selected: true } ); var usernameList = \"\"; for (var i = 0; i < selected.count(); i++) { usernameList += selected.data()[i][0] + ','; } window.location.assign('/payouts?usernameList=' + usernameList + '&dateRange=%s'); } } ] }) });</script><style>div{padding-left:5%%;padding-right:5%%;padding-top:.5%%;padding-bottom:.5%%}</style><div><table class=\"celled table ui\"id=table width=100%%></table></div>"

@RestController
@SpringBootApplication
class TweetGraderApplication {

    private val twitter = TwitterFactory(
            ConfigurationBuilder()
                    .setOAuthConsumerKey("9Si0uS3oOhLVoR9MkUEsrIzde")
                    .setOAuthConsumerSecret("wr1VbvXOBrH4htanXga3HowSGzAuSjfvwZxjFLh2cDZYoNkTCx")
                    .setOAuthAccessToken("1103849475737440261-3sqnrU1JBNiCatCBt1B7yp0fpvfDtp")
                    .setOAuthAccessTokenSecret("M4spF7vq8hz9GeiRfONfpM3xvSqdO8FlkczCHXiYqgjZw")
                    .build()
    ).instance

    @RequestMapping("/")
    fun getIndex(): String {
        // fixme
        return ""
    }

    @RequestMapping("/tweets")
    fun getTweets(@RequestParam startDate: String, @RequestParam postId: String): String {

        val dateFormat = SimpleDateFormat("MM/dd/yyyy")

        val start = Calendar.getInstance()
        start.timeInMillis = dateFormat.parse(startDate).time
        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)

        val end = Calendar.getInstance()
        end.timeInMillis = dateFormat.parse(startDate).time
        end.set(Calendar.HOUR_OF_DAY, 23)
        end.set(Calendar.MINUTE, 59)
        end.set(Calendar.SECOND, 59)
        end.add(Calendar.DAY_OF_YEAR, 6)

        // fixme: jsoup stuff
        val userMap = mapOf(
                Pair("Eco", "fatih_terim_22"),
                Pair("Test", "fatih_terim_22")
        )

        return TWEETS_HTML.format(
                userMap.entries.joinToString(",") { entry ->
                    twitter.timelines().getUserTimeline(entry.value).filter {
                        it.createdAt.time > start.timeInMillis && it.createdAt.time < end.timeInMillis
                    }.joinToString(",") { tweet ->
                        "['${entry.key}', '${tweet.text.replace("'", "’")}', " +
                                "'${tweet.favoriteCount}', '${tweet.retweetCount}', '']"
                    }
                },
                dateFormat.format(start.timeInMillis) +
                        " - " + dateFormat.format(end.timeInMillis)
        )
    }

    @RequestMapping("/payouts")
    fun getPayouts(@RequestParam usernameList: String, @RequestParam dateRange: String): String {

        val usernameCountMap = HashMap<String, Int>()

        usernameList.split(",").forEach { username ->
            if (username.isNotEmpty()) {
                usernameCountMap[username].let { count ->
                    when {
                        count == null -> {
                            usernameCountMap[username] = 1
                        }
                        count < 3 -> {
                            usernameCountMap[username] = count + 1
                        }
                    }
                }
            }
        }

        return "$dateRange<br><br>" + usernameCountMap.entries.joinToString("<br>") {
            it.key + " " + (it.value * 200000)
        }
    }
}

fun main(args: Array<String>) {
    runApplication<TweetGraderApplication>(*args)
}