package net.sr89.haystacker.server.handlers

import net.sr89.haystacker.index.IndexManager
import net.sr89.haystacker.lang.exception.InvalidHslGrammarException
import net.sr89.haystacker.server.api.SearchResponse
import net.sr89.haystacker.server.api.SearchResult
import net.sr89.haystacker.server.api.hslQuery
import net.sr89.haystacker.server.api.indexPath
import net.sr89.haystacker.server.api.maxResults
import net.sr89.haystacker.server.api.searchResponse
import net.sr89.haystacker.server.api.stringBody
import net.sr89.haystacker.server.hslToLucene
import org.apache.lucene.document.Document
import org.apache.lucene.search.ScoreDoc
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import java.nio.file.Paths

class SearchHandler: HttpHandler {
    private fun toSearchResult(document: Document): SearchResult {
        return SearchResult(document.getField("path").stringValue())
    }

    override fun invoke(request: Request): Response {
        val hslQuery: String = hslQuery(request)

        return try {
            val parsedQuery = hslToLucene.toLuceneQuery(hslQuery)
            val maxResults: Int = maxResults(request) ?: 10
            val indexPath: String = indexPath(request)
            val indexManager = IndexManager(indexPath)

            println("Received request to search '$hslQuery' on index $indexPath, returning a maximum of $maxResults results")

            if (!Paths.get(indexPath).toFile().exists()) {
                Response(Status.NOT_FOUND).with(stringBody of "Index at $indexPath not found")
            } else {
                val hits = indexManager.searchIndex(parsedQuery)

                val searchResults = hits.scoreDocs.map(ScoreDoc::doc).mapNotNull(indexManager::fetchDocument).toList()
                    .map { document -> toSearchResult(document) }

                Response(Status.OK).with(
                    searchResponse of SearchResponse(hits.totalHits.value, hits.scoreDocs.size, searchResults)
                )
            }
        } catch (e: InvalidHslGrammarException) {
            Response(Status.BAD_REQUEST).with(stringBody of "Unable to parse query $hslQuery: ${e.message}")
        }
    }
}