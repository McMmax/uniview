# 📺 Univue

**Univue** (Universal View) is a comprehensive UI framework designed specifically for **Minestom**.

---

## 🎯 What does it do?
Creating complex, interactive menus (like chest inventories, anvils, or custom GUIs) in Minecraft is historically painful. You have to manually track click slots, manage item states, and handle network desyncs.

Univue abstracts all of this away into a modern, declarative "View" architecture. You define *what* the UI should look like and how it should react, and Univue handles all the messy inventory math and packet listening in the background.

## 🏗️ Architecture

- **`univue-core`**: The standalone framework that defines Views, States, and Event bindings independently of any specific Minecraft implementation.
- **`univue-minestom`**: The Minestom-specific bindings that translate Univue's abstract "Views" into actual Minecraft Inventory packets and window click events.

## 🚀 Key Features
- **State-Driven**: UI components automatically update when their underlying state data changes, much like React or Vue.js.
- **Event-Binding**: Cleanly bind click, drag, and drop events to specific items without calculating slot numbers.
- **Modular**: Build small UI "Widgets" (like a pagination footer) and re-use them across completely different menus.
