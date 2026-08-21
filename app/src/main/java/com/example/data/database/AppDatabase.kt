package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.HistoryDao
import com.example.data.dao.HostDao
import com.example.data.dao.KeyDao
import com.example.data.dao.SnippetDao
import com.example.data.entity.HistoryEntity
import com.example.data.entity.HostEntity
import com.example.data.entity.KeyEntity
import com.example.data.entity.SnippetEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        HostEntity::class,
        KeyEntity::class,
        SnippetEntity::class,
        HistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun hostDao(): HostDao
    abstract fun keyDao(): KeyDao
    abstract fun snippetDao(): SnippetDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "binbox_terminal.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            seedInitialData(getInstance(context))
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedInitialData(db: AppDatabase) {
            // Seed Hosts
            db.hostDao().insertHost(
                HostEntity(
                    label = "Cloud Linux Sandbox (Demo)",
                    host = "vps-demo.binbox.io",
                    port = 22,
                    protocol = "DEMO_HOST",
                    username = "root",
                    authType = "PASSWORDLESS",
                    groupTag = "Cloud",
                    themeId = "cyberpunk",
                    isFavorite = true,
                    lastLatencyMs = 12
                )
            )

            db.hostDao().insertHost(
                HostEntity(
                    label = "Local Android Shell",
                    host = "localhost",
                    port = 0,
                    protocol = "LOCAL_SHELL",
                    username = "shell",
                    authType = "PASSWORDLESS",
                    groupTag = "Local",
                    themeId = "monokai_pro",
                    isFavorite = true,
                    lastLatencyMs = 1
                )
            )

            db.hostDao().insertHost(
                HostEntity(
                    label = "HomeLab Raspberry Pi 5",
                    host = "192.168.1.150",
                    port = 22,
                    protocol = "SSH",
                    username = "pi",
                    authType = "PASSWORD",
                    password = "raspberrypi",
                    groupTag = "HomeLab",
                    themeId = "nord",
                    isFavorite = false,
                    lastLatencyMs = 8
                )
            )

            db.hostDao().insertHost(
                HostEntity(
                    label = "Production Web Cluster",
                    host = "10.0.0.12",
                    port = 22,
                    protocol = "SSH",
                    username = "ubuntu",
                    authType = "PRIVATE_KEY",
                    groupTag = "Production",
                    themeId = "dracula",
                    isFavorite = false,
                    lastLatencyMs = 24
                )
            )

            // Seed Snippets
            val snippets = listOf(
                SnippetEntity(
                    title = "FastFetch System Info",
                    commandTemplate = "fastfetch || neofetch",
                    category = "System",
                    description = "Visual hardware, kernel, memory, and OS summary",
                    isFavorite = true
                ),
                SnippetEntity(
                    title = "Live CPU & RAM Monitor",
                    commandTemplate = "htop || top",
                    category = "System",
                    description = "Interactive process viewer and load stats",
                    isFavorite = true
                ),
                SnippetEntity(
                    title = "Docker Active Containers",
                    commandTemplate = "docker ps --format \"table {{.Names}}\\t{{.Status}}\\t{{.Ports}}\"",
                    category = "Docker",
                    description = "Clean table of running container services",
                    isFavorite = true
                ),
                SnippetEntity(
                    title = "Follow Application Log",
                    commandTemplate = "tail -n {{lines:50}} -f {{logfile:/var/log/syslog}}",
                    category = "System",
                    description = "Stream live system or app log messages"
                ),
                SnippetEntity(
                    title = "Disk Space Partition Usage",
                    commandTemplate = "df -hT --exclude-type=tmpfs",
                    category = "System",
                    description = "Inspect free and used disk capacity"
                ),
                SnippetEntity(
                    title = "Active Listening Network Ports",
                    commandTemplate = "ss -tulwn || netstat -tulnp",
                    category = "Network",
                    description = "Show all listening TCP and UDP sockets"
                ),
                SnippetEntity(
                    title = "Memory Stats in Megabytes",
                    commandTemplate = "free -m -h",
                    category = "System",
                    description = "Inspect RAM and Swap memory usage"
                ),
                SnippetEntity(
                    title = "Git Fetch & Rebase",
                    commandTemplate = "git status && git pull --rebase",
                    category = "Git",
                    description = "Inspect repo status and sync latest changes"
                ),
                SnippetEntity(
                    title = "Docker Container Logs",
                    commandTemplate = "docker logs -f --tail {{lines:100}} {{container_name:frontend-proxy}}",
                    category = "Docker",
                    description = "Tail stdout and stderr from docker container"
                ),
                SnippetEntity(
                    title = "Network Latency Ping",
                    commandTemplate = "ping -c 4 {{host:1.1.1.1}}",
                    category = "Network",
                    description = "Test ICMP round-trip latency to a host"
                ),
                SnippetEntity(
                    title = "Docker System Prune",
                    commandTemplate = "docker system prune -f",
                    category = "Docker",
                    description = "Reclaim disk by removing unused docker artifacts"
                )
            )

            snippets.forEach { db.snippetDao().insertSnippet(it) }
        }
    }
}
