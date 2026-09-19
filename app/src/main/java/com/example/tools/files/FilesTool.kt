package com.example.tools.files

import android.content.Context
import com.example.tools.AndroidTool
import com.example.tools.RiskLevel
import com.example.tools.ToolDefinition
import com.example.tools.ToolParameter
import com.example.tools.ToolResult
import java.io.File

class FilesTool : AndroidTool {
    override val definition: ToolDefinition = ToolDefinition(
        name = "manage_files",
        description = "Read, write, create, list, or search text files in the Hermes local workspace directory",
        riskLevel = RiskLevel.MEDIUM,
        parameters = listOf(
            ToolParameter(
                name = "action",
                type = "string",
                description = "File operation: 'list', 'read', 'write', 'search', 'delete'",
                required = true,
                enumValues = listOf("list", "read", "write", "search", "delete")
            ),
            ToolParameter(
                name = "file_name",
                type = "string",
                description = "Name of the file (e.g. 'notes.txt', 'shopping_list.md')",
                required = false
            ),
            ToolParameter(
                name = "content",
                type = "string",
                description = "Content to write or append to the file",
                required = false
            ),
            ToolParameter(
                name = "query",
                type = "string",
                description = "Search query for searching files",
                required = false
            )
        )
    )

    override suspend fun execute(context: Context, arguments: Map<String, Any?>): ToolResult {
        val action = arguments["action"]?.toString()?.lowercase() ?: "list"
        val fileName = arguments["file_name"]?.toString()?.trim() ?: ""
        val content = arguments["content"]?.toString() ?: ""
        val query = arguments["query"]?.toString() ?: ""

        val workspaceDir = File(context.filesDir, "hermes_workspace").apply { mkdirs() }

        return try {
            when (action) {
                "list" -> {
                    val files = workspaceDir.listFiles()?.map {
                        mapOf(
                            "name" to it.name,
                            "size_bytes" to it.length(),
                            "last_modified" to it.lastModified()
                        )
                    } ?: emptyList()
                    ToolResult.success(
                        data = mapOf("files" to files, "count" to files.size),
                        userNotice = "Found ${files.size} file(s)"
                    )
                }

                "read" -> {
                    if (fileName.isEmpty()) return ToolResult.failure("file_name is required for read action.")
                    val targetFile = File(workspaceDir, sanitizeFileName(fileName))
                    if (!targetFile.exists()) {
                        return ToolResult.failure("File '$fileName' does not exist in Hermes workspace.")
                    }
                    val text = targetFile.readText()
                    ToolResult.success(
                        data = mapOf("file_name" to fileName, "content" to text),
                        userNotice = "Read $fileName"
                    )
                }

                "write" -> {
                    if (fileName.isEmpty()) return ToolResult.failure("file_name is required for write action.")
                    val targetFile = File(workspaceDir, sanitizeFileName(fileName))
                    targetFile.writeText(content)
                    ToolResult.success(
                        data = mapOf(
                            "file_name" to fileName,
                            "bytes_written" to targetFile.length(),
                            "status" to "saved"
                        ),
                        userNotice = "Saved $fileName"
                    )
                }

                "search" -> {
                    val matching = workspaceDir.listFiles { file ->
                        file.name.contains(query, ignoreCase = true) ||
                                (file.isFile && file.readText().contains(query, ignoreCase = true))
                    }?.map { it.name } ?: emptyList()

                    ToolResult.success(
                        data = mapOf("query" to query, "matches" to matching),
                        userNotice = "Search matched ${matching.size} file(s)"
                    )
                }

                "delete" -> {
                    if (fileName.isEmpty()) return ToolResult.failure("file_name is required for delete action.")
                    val targetFile = File(workspaceDir, sanitizeFileName(fileName))
                    if (!targetFile.exists()) return ToolResult.failure("File '$fileName' not found.")
                    val deleted = targetFile.delete()
                    ToolResult.success(
                        data = mapOf("file_name" to fileName, "deleted" to deleted),
                        userNotice = "Deleted $fileName"
                    )
                }

                else -> ToolResult.failure("Unknown file action: $action")
            }
        } catch (e: Exception) {
            ToolResult.failure("File operation error: ${e.localizedMessage}")
        }
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }
}
