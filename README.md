# simpleHashMap

> 手写一个 HashMap —— 用「数组 + 链表」把 JDK `HashMap` 的底层结构和扩容机制拆开看清楚。

| 项目 | 说明 |
| --- | --- |
| 语言 | Java（无任何第三方依赖） |
| 环境 | JDK 8 及以上（实测 JDK 25.0.3） |
| 代码量 | 单文件，约 145 行 |
| 定位 | 数据结构学习 / 面试准备 |

---

## 项目简介

`SimpleHashMap` 是从零实现的哈希表，目的在于理解 JDK `HashMap` 的四个核心问题：

1. 哈希冲突用什么结构解决（本项目：链地址法）
2. 负载因子与扩容阈值怎么配合
3. `null` key 往哪里放
4. 什么时候扩容、扩容时旧节点怎么迁移

## 特性

- ✅ 泛型 `SimpleHashMap<K, V>`
- ✅ `put` / `get` / `size`，`put` 返回被覆盖的旧值
- ✅ 链地址法（Separate Chaining）解决哈希冲突，新节点采用**头插**
- ✅ 支持 `null` key（固定落在 `table[0]`）
- ✅ 负载因子 0.75，元素数超过阈值自动扩容（容量翻倍）
- ✅ 下标计算已处理负 `hashCode`，`put(-1, ...)` 不会越界
- ⚠️ 非线程安全，其余限制见 [已知限制](#已知限制)

## 目录结构

```
simpleHashMap/
├── src/
│   └── com/hashmap/
│       └── SimpleHashMap.java   # 全部实现（含示例 main）
├── out/                         # 编译输出，已在 .gitignore 中忽略
├── hashmap.iml                  # IntelliJ IDEA 模块文件
├── .gitignore
└── README.md
```

## 快速开始

### 编译与运行

```bash
git clone https://github.com/Champagne-wmh/simpleHashMap.git
cd simpleHashMap

# 编译
javac -d out src/com/hashmap/SimpleHashMap.java

# 运行示例
java -cp out com.hashmap.SimpleHashMap
```

预期输出：

```
99
2
1
3
```

对应 `main` 里的四步：`get("a")` 拿到了被覆盖后的 `99`、`get("b")` 得 `2`、`get(null)` 得 `1`、三个不同的键（`a` / `b` / `null`）使 `size()` 为 `3`。

### 作为库使用

```java
SimpleHashMap<String, Integer> map = new SimpleHashMap<>();

map.put("a", 1);
map.put("b", 2);
System.out.println(map.put("a", 99));  // 1，返回被覆盖的旧值
System.out.println(map.get("a"));      // 99
System.out.println(map.size());        // 2
```

---

## 核心实现

### 数据结构

容器本身是一个数组，数组的每个槽位（桶）指向一条单链表：

```
index = (hash & 0x7FFFFFFF) % table.length

table
 ┌────────┐
0│ null   │
 ├────────┤
1│ ●──► Node(k,v) ──► Node(k,v) ──► null
 ├────────┤
2│ null   │
 ├────────┤
 ...
 ├────────┤
15│ ●──► Node(k,v) ──► null
 └────────┘
```

同一个桶内的节点用 `next` 串起来，查找时先比 `hashCode()` 再比 `equals()`。

### 字段说明

| 字段 | 类型 | 含义 |
| --- | --- | --- |
| `table` | `Node<K, V>[]` | 桶数组，初始容量 16 |
| `size` | `int` | 已存储的键值对数量 |
| `loadFactor` | `float` | 负载因子，固定 0.75 |
| `threshold` | `int` | 扩容阈值 = 容量 × 负载因子（初始 12） |

### `put` 流程

```
put(key, value)
  │
  ├─ key == null ──────────────────► putForNullKey(value)   // 存到 table[0]
  │
  ├─ hash  = key.hashCode()
  │  index = (hash & 0x7FFFFFFF) % table.length
  │
  ├─ 遍历 table[index] 链表
  │    └─ 命中 (hashCode 相同 && equals 相等) ──► 覆盖 value，返回旧值
  │
  ├─ 未命中 ──► 头插新节点到 table[index]，size++
  │
  └─ size > threshold ──► resize()
```

### `get` 流程

与 `put` 的查找部分一致：定位桶 → 遍历链表 → 命中返回 `value`，否则返回 `null`；`key == null` 时走 `getForNullKey()`。

### `resize` 扩容流程

1. 容量翻倍（`newCap = oldCap * 2`）并创建新数组
2. 按新容量重算 `threshold`
3. 遍历旧表的每个桶，对链上每个节点**重新计算下标**，头插到新表

---

## API

| 方法 | 说明 |
| --- | --- |
| `V put(K key, V value)` | 存入或覆盖，返回被覆盖的旧值（无旧值返回 `null`） |
| `V get(K key)` | 按键取值，不存在返回 `null` |
| `int size()` | 返回当前键值对数量 |
| `V putForNullKey(V value)` | 内部使用，处理 `null` key |
| `V getForNullKey()` | 内部使用，读取 `null` key |
| `void resize()` | 内部使用，扩容并迁移节点 |

## 复杂度

| 操作 | 平均 | 最坏 |
| --- | --- | --- |
| `put` | O(1) | O(n) |
| `get` | O(1) | O(n) |

最坏情况出现在所有 key 的哈希值都落在同一个桶时，链表退化为线性查找。扩容单次开销为 O(n)，均摊后为 O(1)。空间复杂度 O(n)。

---

## 与 JDK `HashMap` 的差异

| 维度 | 本实现 | JDK 8+ `HashMap` |
| --- | --- | --- |
| 哈希扰动 | 直接用 `key.hashCode()` | `h ^ (h >>> 16)`，让高 16 位参与运算 |
| 下标计算 | `(hash & 0x7FFFFFFF) % length` | `(n - 1) & hash`，因此容量必须是 2 的幂 |
| 冲突结构 | 仅单链表 | 链表；长度 ≥ 8 且容量 ≥ 64 时转红黑树 |
| 扩容迁移 | 每个节点重算 `(hash & 0x7FFFFFFF) % newCap` | 用 `(e.hash & oldCap) == 0` 判断留在原下标或 `oldCap + index`，无需重算哈希 |
| 容量初始化 | 构造时即分配 16 | 首次 `put` 时懒加载 |
| 插入方式 | 头插 | 尾插 |
| `null` key | 放入 `table[0]` | 放入 `table[0]`，哈希值固定为 0 |
| 线程安全 | 否 | 否（对应 `ConcurrentHashMap`） |

---


## 已知限制

- **非线程安全**：并发 `put` 可能丢数据，多线程场景应使用 `ConcurrentHashMap`
- **`get` 返回 `null` 有歧义**：无法区分「键不存在」和「值本身是 null」，这也是 JDK 需要 `containsKey()` 的原因
- **接口不全**：`remove` / `containsKey` / `isEmpty` / `clear` / 遍历（`keySet` / `entrySet`）尚未实现
- **无单元测试**

---

## TODO

- [ ] 补充 `remove`、`containsKey`、`isEmpty`、`clear`
- [ ] 用 JUnit 5 补单元测试，覆盖哈希冲突、扩容、`null` key、负数 `hashCode`
- [ ] 对齐 JDK 8 优化：链表长度 ≥ 8 时转红黑树
- [ ] 改用 `hash & (n - 1)` 取模，把容量严格约束为 2 的幂
- [ ] 加入哈希扰动函数 `h ^ (h >>> 16)`，减少碰撞
- [ ] 实现线程安全版本，对比 `ConcurrentHashMap` 的 CAS / 分段思路

---

## 说明

个人学习练习项目，暂未指定开源协议。

