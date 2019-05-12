package com.nsfl.tweetgrader

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import twitter4j.Paging
import twitter4j.Status
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

private const val TWEETS_HTML = "<!doctypehtml><link href=https://cdn.datatables.net/1.10.19/css/jquery.dataTables.min.css rel=stylesheet><link href=https://cdn.datatables.net/select/1.3.0/css/select.dataTables.min.css rel=stylesheet><link href=https://cdn.datatables.net/buttons/1.5.6/css/buttons.dataTables.min.css rel=stylesheet><script src=https://code.jquery.com/jquery-3.3.1.js></script><script src=https://cdn.datatables.net/1.10.19/js/jquery.dataTables.min.js></script><script src=https://cdn.datatables.net/select/1.3.0/js/dataTables.select.min.js></script><script src=https://cdn.datatables.net/buttons/1.5.6/js/dataTables.buttons.min.js></script><script class=init>var dataSet = [%s]; \$(document).ready(function() { var table = \$('#table').DataTable({ paging: false, order: [ [0, \"asc\"] ], data: dataSet, columns: [ { title: 'Username' }, { title: 'Account' }, { title: 'Tweet' }, { title: 'Content' }, { title: 'Likes' }, { title: 'Retweets' }, { title: '' }], columnDefs: [ { orderable: false, className: 'select-checkbox', targets: 6 } ], select: { style: 'multi', selector: 'td:last-child' }, dom: 'Bfrtip', buttons: [ 'selectAll', 'selectNone' , { text: 'Generate payouts', action: function () { var selected = table.rows( { selected: true } ); var usernameList = \"\"; for (var i = 0; i < selected.count(); i++) { usernameList += selected.data()[i][0] + ','; } window.location.assign('/payouts?usernameList=' + usernameList + '&dateRange=%s'); } } ] }) });</script><style>div{padding-left:5%%;padding-right:5%%;padding-top:.5%%;padding-bottom:.5%%}</style><div><table class=\"cell-border\"id=table width=100%%></table></div>"

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
        return "<form action=/tweets>Post URL<br><input name=postUrl><br><br>Start Date (Sunday)<br><input name=startDate type=date><br><br><input type=submit></form>"
    }

    @RequestMapping("/tweets")
    fun getTweets(@RequestParam startDate: String, @RequestParam postUrl: String): String {

        val inputDateFormat = SimpleDateFormat("yyyy-MM-dd")
        val outputDateFormat = SimpleDateFormat("MM/dd/yyyy")

        val start = Calendar.getInstance()
        start.timeInMillis = inputDateFormat.parse(startDate).time
        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)

        val end = Calendar.getInstance()
        end.timeInMillis = inputDateFormat.parse(startDate).time
        end.set(Calendar.HOUR_OF_DAY, 23)
        end.set(Calendar.MINUTE, 59)
        end.set(Calendar.SECOND, 59)
        end.add(Calendar.DAY_OF_YEAR, 6)

        val userMap = HashMap<String, String>()
        val documentList = ArrayList<Document>()

        val firstDocument = Jsoup.connect(postUrl).get()
        val firstDocumentBody = firstDocument.body().toString()
        documentList.add(firstDocument)

        val pageCount = try {
            val startIndex = firstDocumentBody.indexOf("Pages:</a>")
            val endIndex = firstDocumentBody.indexOf(")", startIndex)
            firstDocumentBody.substring(startIndex, endIndex)
                    .replace(Pattern.compile("[^0-9.]").toRegex(), "")
                    .toInt()
        } catch (exception: Exception) {
            1
        }

        for (i in 1..(pageCount - 1)) {
            documentList.add(
                    Jsoup.connect(
                            "$postUrl&st=${i * 14}"
                    ).get()
            )
        }

        documentList.forEach {
            it.body().getElementsByClass("post-normal").forEach { element ->
                val username = element.getElementsByClass("normalname").text()
                val content = element.getElementsByClass("postcolor").toString()
                if (content.contains("twitter.com/")) {
                    var completed = false
                    var index = content.indexOf("twitter.com/") + 12
                    var twitter = ""
                    while (!completed) {
                        val char = content[index]
                        if (char != '"' && char != '<' && char != '/' && char != '?' && char != ' ') {
                            twitter += char
                        } else {
                            completed = true
                        }
                        index++
                        if (index == content.length) {
                            completed = true
                        }
                    }
                    userMap[username] = twitter
                }
            }
        }

        return TWEETS_HTML.format(userMap.entries.joinToString(",") { entry ->

            val tweetList = try {
                twitter.timelines().getUserTimeline(entry.value, Paging(1, 200)).map { it }.filter {
                    it.createdAt.time > start.timeInMillis && it.createdAt.time < end.timeInMillis
                }
            } catch (exception: Exception) {
                arrayListOf<Status>()
            }

            if (tweetList.isEmpty()) {
                "['${entry.key.replace("'", "\\'")}', '', '', " +
                        "'<font color=\"red\"><b>Could not parse this user’s tweets</b></font>.', '0', '0', '']"
            } else {
                tweetList.joinToString(",") { tweet ->
                    "['${entry.key.replace("'", "\\'")}', " +
                            "'<a href=\"https://www.twitter.com/${entry.value}\">Link</a>', " +
                            "'<a href=\"https://www.twitter.com/${entry.value}/status/${tweet.id}\">Link</a>', " +
                            "'${tweet.text.replace("'", "\\'").replace("\n", " ")}', " +
                            "'${tweet.favoriteCount}', " +
                            "'${tweet.retweetCount}', " +
                            "'']"
                }
            }
        },
                outputDateFormat.format(start.timeInMillis) +
                        " - " + outputDateFormat.format(end.timeInMillis)
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