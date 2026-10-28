package com.nationnums.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val NationBlue = Color(0xFF145DDB)
private val Ink = Color(0xFF14213D)
private val Muted = Color(0xFF6C7890)
private val Canvas = Color(0xFFF5F7FC)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = NationBlue,
                    background = Canvas,
                    surface = Color.White,
                    onBackground = Ink,
                    onSurface = Ink
                )
            ) {
                NationNumsApp()
            }
        }
    }
}

private enum class AppTab(val label: String) {
    Home("Home"), Numbers("Numbers"), Orders("Orders"), Wallet("Wallet"), Profile("Profile")
}

@Composable
private fun NationNumsApp() {
    var selectedTab by remember { mutableStateOf(AppTab.Home) }
    Surface(modifier = Modifier.fillMaxSize(), color = Canvas) {
        Column(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                AppTab.Home -> Dashboard(onFund = { selectedTab = AppTab.Wallet }, onBuy = { selectedTab = AppTab.Numbers })
                AppTab.Numbers -> SimplePage("Buy a number", "Choose a country, service and available provider option.", "Browse numbers")
                AppTab.Orders -> SimplePage("Your orders", "Your purchased numbers and received OTP messages will appear here after account connection.", "Refresh orders")
                AppTab.Wallet -> SimplePage("Wallet", "Wallet balance and payment options will connect to your Nation Nums account.", "Fund wallet")
                AppTab.Profile -> SimplePage("Your profile", "Sign in to access your account, wallet and order history.", "Sign in")
            }
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = Color.White
            ) {
                AppTab.entries.forEach { tab ->
                    val icon = when (tab) {
                        AppTab.Home -> Icons.Rounded.Home
                        AppTab.Numbers -> Icons.Rounded.PhoneAndroid
                        AppTab.Orders -> Icons.Rounded.History
                        AppTab.Wallet -> Icons.Rounded.Wallet
                        AppTab.Profile -> Icons.Rounded.AccountCircle
                    }
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontSize = 10.sp) }
                    )
                }
            }
        }
    }
}

@Composable
private fun Dashboard(onFund: () -> Unit, onBuy: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(46.dp).background(NationBlue, RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.PhoneAndroid, contentDescription = null, tint = Color.White, modifier = Modifier.size(27.dp))
                }
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("Nation Nums", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text("Your numbers. One dashboard.", fontSize = 12.sp, color = Muted)
                }
                Icon(Icons.Rounded.AccountCircle, contentDescription = "Profile", tint = NationBlue, modifier = Modifier.size(30.dp))
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = NationBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(22.dp)) {
                    Text("WALLET BALANCE", color = Color.White.copy(alpha = 0.78f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("₦ ———", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text("Sign in to load your live balance", color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp)
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = onFund,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = NationBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Text("  Fund wallet", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            Text("Quick actions", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction("Buy number", "Choose service", Icons.Rounded.PhoneAndroid, onBuy, Modifier.weight(1f))
                QuickAction("My orders", "View OTPs", Icons.Rounded.History, {}, Modifier.weight(1f))
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Welcome to Nation Nums", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "The native Android app is being connected to your existing Nation Nums account. Live wallet, payments, number availability and orders will be enabled after the backend connection is verified.",
                        color = Muted, fontSize = 13.sp, lineHeight = 19.sp
                    )
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun QuickAction(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(16.dp)) {
            Box(Modifier.size(42.dp).background(Color(0xFFEAF1FF), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = NationBlue)
            }
            Spacer(Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.Bold, color = Ink, fontSize = 14.sp)
            Text(subtitle, color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SimplePage(title: String, description: String, action: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text(title, fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(10.dp))
        Text(description, fontSize = 15.sp, lineHeight = 22.sp, color = Muted)
        Spacer(Modifier.height(22.dp))
        Button(onClick = {}, shape = RoundedCornerShape(12.dp)) { Text(action) }
    }
}
