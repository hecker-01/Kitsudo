package dev.heckr.kitsudo.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

/**
 * Minimal CommonMark-subset renderer for GitHub release notes.
 *
 * Deliberately not a full Markdown engine - it covers what release bodies
 * actually contain: ATX headings, bullet / numbered lists, fenced code blocks,
 * horizontal rules, and inline bold / italic / code / links. A dedicated
 * library was avoided to stay consistent with the project's "no dep for a
 * tiny feature" stance (cf. HttpURLConnection in UpdateChecker).
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val codeBg = MaterialTheme.colorScheme.surfaceVariant
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Blank -> Spacer(Modifier.height(6.dp))
                is MdBlock.Divider -> HorizontalDivider(Modifier.padding(vertical = 4.dp))
                is MdBlock.Heading -> Text(
                    text = inlineAnnotated(block.text, linkColor, codeBg),
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.titleMedium
                        2 -> MaterialTheme.typography.titleSmall
                        else -> MaterialTheme.typography.bodyLarge
                    },
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.padding(top = 4.dp),
                )
                is MdBlock.Paragraph -> Text(
                    text = inlineAnnotated(block.text, linkColor, codeBg),
                    style = MaterialTheme.typography.bodyMedium,
                    color = color,
                )
                is MdBlock.Code -> Text(
                    text = block.text,
                    style = MaterialTheme.typography.bodySmall
                        .copy(fontFamily = FontFamily.Monospace),
                    color = color,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                )
                is MdBlock.ListItem -> Row(
                    modifier = Modifier.padding(
                        PaddingValues(start = (block.indent * 16).dp),
                    ),
                ) {
                    Text(
                        text = "${block.marker}  ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = color,
                    )
                    Text(
                        text = inlineAnnotated(block.text, linkColor, codeBg),
                        style = MaterialTheme.typography.bodyMedium,
                        color = color,
                    )
                }
            }
        }
    }
}

// -- Block model ------------------------------------------------------------

private sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class ListItem(val indent: Int, val marker: String, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class Code(val text: String) : MdBlock
    data object Divider : MdBlock
    data object Blank : MdBlock
}

private val headingRegex = Regex("^(#{1,6})\\s+(.*)$")
private val bulletRegex = Regex("^(\\s*)([-*+])\\s+(.*)$")
private val orderedRegex = Regex("^(\\s*)(\\d+)[.)]\\s+(.*)$")
private val ruleRegex = Regex("^\\s*([-*_])\\s*(\\1\\s*){2,}$")

private fun parseMarkdownBlocks(markdown: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val lines = markdown.replace("\r\n", "\n").replace("\r", "\n").split("\n")

    var i = 0
    while (i < lines.size) {
        val line = lines[i]

        // Fenced code block: collect verbatim until the closing fence.
        if (line.trimStart().startsWith("```")) {
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            i++ // skip closing fence (or run off the end)
            blocks.add(MdBlock.Code(codeLines.joinToString("\n")))
            continue
        }

        when {
            line.isBlank() -> blocks.add(MdBlock.Blank)
            ruleRegex.matches(line) -> blocks.add(MdBlock.Divider)
            headingRegex.matches(line) -> {
                val m = headingRegex.find(line)!!
                blocks.add(MdBlock.Heading(m.groupValues[1].length, m.groupValues[2].trim()))
            }
            bulletRegex.matches(line) -> {
                val m = bulletRegex.find(line)!!
                blocks.add(MdBlock.ListItem(indentLevel(m.groupValues[1]), "•", m.groupValues[3]))
            }
            orderedRegex.matches(line) -> {
                val m = orderedRegex.find(line)!!
                blocks.add(MdBlock.ListItem(indentLevel(m.groupValues[1]), "${m.groupValues[2]}.", m.groupValues[3]))
            }
            else -> blocks.add(MdBlock.Paragraph(line.trim()))
        }
        i++
    }
    // Collapse runs of blanks so spacing stays even.
    return blocks.filterIndexed { idx, b ->
        b !is MdBlock.Blank || (idx > 0 && blocks[idx - 1] !is MdBlock.Blank)
    }
}

/** Two leading spaces (or one tab) ≈ one nesting level, capped to keep indent sane. */
private fun indentLevel(leading: String): Int =
    (leading.replace("\t", "  ").length / 2).coerceIn(0, 3)

// -- Inline parsing ---------------------------------------------------------

private val linkRegex = Regex("\\[([^\\]]+)]\\(([^)]+)\\)")
private val boldRegex = Regex("\\*\\*(.+?)\\*\\*")
private val codeSpanRegex = Regex("`([^`]+)`")
private val italicRegex = Regex("(?<![*_\\w])[*_]([^*_\\n]+)[*_](?![*_\\w])")

/** Builds an [AnnotatedString] for one line, resolving inline markup recursively. */
private fun inlineAnnotated(text: String, linkColor: Color, codeBg: Color): AnnotatedString =
    buildAnnotatedString { appendInline(text, linkColor, codeBg) }

private fun AnnotatedString.Builder.appendInline(text: String, linkColor: Color, codeBg: Color) {
    var i = 0
    while (i < text.length) {
        val candidates = listOfNotNull(
            linkRegex.find(text, i),
            boldRegex.find(text, i),
            codeSpanRegex.find(text, i),
            italicRegex.find(text, i),
        ).filter { it.range.first >= i }

        val next = candidates.minByOrNull { it.range.first }
        if (next == null) {
            append(text.substring(i))
            return
        }
        if (next.range.first > i) append(text.substring(i, next.range.first))

        when {
            next.value.startsWith("[") -> {
                val label = next.groupValues[1]
                val url = next.groupValues[2]
                withLink(
                    LinkAnnotation.Url(
                        url,
                        TextLinkStyles(
                            SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline,
                            ),
                        ),
                    ),
                ) { appendInline(label, linkColor, codeBg) }
            }
            next.value.startsWith("**") ->
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    appendInline(next.groupValues[1], linkColor, codeBg)
                }
            next.value.startsWith("`") ->
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg)) {
                    append(next.groupValues[1])
                }
            else ->
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    appendInline(next.groupValues[1], linkColor, codeBg)
                }
        }
        i = next.range.last + 1
    }
}
