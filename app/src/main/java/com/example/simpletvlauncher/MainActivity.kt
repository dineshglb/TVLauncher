package com.example.simpletvlauncher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView

class MainActivity : Activity() {

    data class AppItem(val label: String, val pkg: String, val icon: Drawable, val launch: Intent)

    private lateinit var grid: GridView
    private lateinit var toggle: TextView
    private lateinit var prefs: SharedPreferences
    private val adapter = Adapter()
    private val apps = mutableListOf<AppItem>()
    private var hidden: Set<String> = emptySet()
    private var showHidden = false
    private var swallowOkUp = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("launcher", Context.MODE_PRIVATE)

        grid = findViewById(R.id.grid)
        toggle = findViewById(R.id.toggle)
        grid.selector = getDrawable(R.drawable.selector_grid)
        grid.adapter = adapter
        grid.setOnItemClickListener { _, _, pos, _ -> startActivity(apps[pos].launch) }

        toggle.setOnClickListener {
            showHidden = !showHidden
            loadApps()
        }
    }

    override fun onResume() {
        super.onResume()
        loadApps()
    }

    private fun loadApps() {
        hidden = prefs.getStringSet("hidden", emptySet())!!.toSet()
        toggle.text = if (showHidden) "Hidden apps: SHOWN" else "Hidden apps: OFF"

        val found = LinkedHashMap<String, android.content.pm.ResolveInfo>()
        for (cat in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val q = Intent(Intent.ACTION_MAIN).addCategory(cat)
            packageManager.queryIntentActivities(q, 0)
                .forEach { found.putIfAbsent(it.activityInfo.packageName, it) }
        }

        apps.clear()
        found.values
            .filter { it.activityInfo.packageName != packageName }
            .filter { showHidden || it.activityInfo.packageName !in hidden }
            .map {
                val a = it.activityInfo
                AppItem(
                    label = it.loadLabel(packageManager).toString(),
                    pkg = a.packageName,
                    icon = it.loadIcon(packageManager),
                    launch = Intent(Intent.ACTION_MAIN)
                        .setClassName(a.packageName, a.name)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            .sortedBy { it.label.lowercase() }
            .let { apps.addAll(it) }
        adapter.notifyDataSetChanged()
    }

    private fun toggleHidden(pkg: String) {
        val set = prefs.getStringSet("hidden", emptySet())!!.toMutableSet()
        if (!set.add(pkg)) set.remove(pkg)
        prefs.edit().putStringSet("hidden", set).apply()
        loadApps()
    }

    // Hold OK / Select on an app to hide or unhide it
    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        val isOk = e.keyCode == KeyEvent.KEYCODE_DPAD_CENTER || e.keyCode == KeyEvent.KEYCODE_ENTER
        if (isOk && grid.hasFocus()) {
            if (e.action == KeyEvent.ACTION_DOWN && e.repeatCount > 0) {
                if (e.repeatCount == 1) {
                    val pos = grid.selectedItemPosition
                    if (pos in apps.indices) toggleHidden(apps[pos].pkg)
                    swallowOkUp = true
                }
                return true
            }
            if (e.action == KeyEvent.ACTION_UP && swallowOkUp) {
                swallowOkUp = false
                return true
            }
        }
        return super.dispatchKeyEvent(e)
    }

    @Deprecated("Launcher stays on Home")
    override fun onBackPressed() { /* a launcher should not exit */ }

    inner class Adapter : BaseAdapter() {
        override fun getCount() = apps.size
        override fun getItem(position: Int) = apps[position]
        override fun getItemId(position: Int) = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val v = convertView ?: layoutInflater.inflate(R.layout.item_app, parent, false)
            val app = apps[position]
            v.findViewById<ImageView>(R.id.icon).setImageDrawable(app.icon)
            v.findViewById<TextView>(R.id.label).text = app.label
            v.alpha = if (app.pkg in hidden) 0.4f else 1f
            return v
        }
    }
}
