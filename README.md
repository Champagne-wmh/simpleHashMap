# simpleHashMap

> 手写一个 HashMap —— 用「数组 + 链表」把 JDK `HashMap` 的底层结构和扩容机制拆开看清楚。

| 项目 | 说明 |
| --- | --- |
| 语言 | Java（无任何第三方依赖） |
| 实测环境 | JDK 25.0.3 |
| 代码量 | 单文件，约 140 行 |
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
- ✅ 负载因子 0.75，元素数超过阈值自动扩容
- ⚠️ 非线程安全；且目前存在若干已确认的逻辑缺陷，见 [已知问题](#已知问题)

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

### 环境要求

- JDK 25（示例 `main` 使用了无参 `static void main()`，这是 JDK 25 的 compact main 写法）
- 若使用 JDK 17 / 21，请先把 `main` 改成 `public static void main(String[] args)`

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
null
3
```

> 第 3 行 `null` 不是笔误 —— `get(null)` 目前读不到 `put(null, 1)` 存进去的值，原因见 [已知问题](#已知问题)。

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
index = hash % table.length

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
  ├─ hash = key.hashCode()
  │  index = hash % table.length
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

1. 计算新容量并创建新数组
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
| 下标计算 | `hash % length` | `(n - 1) & hash`，因此容量必须是 2 的幂 |
| 冲突结构 | 仅单链表 | 链表；长度 ≥ 8 且容量 ≥ 64 时转红黑树 |
| 扩容迁移 | 每个节点重算 `hash % newCap` | 用 `(e.hash & oldCap) == 0` 判断留在原下标或 `oldCap + index`，无需重算哈希 |
| 容量初始化 | 构造时即分配 16 | 首次 `put` 时懒加载 |
| 插入方式 | 头插 | 尾插 |
| `null` key | 放入 `table[0]` | 放入 `table[0]`，哈希值固定为 0 |
| 线程安全 | 否 | 否（对应 `ConcurrentHashMap`） |

---

## 已知问题

以下问题均已在本机（JDK 25）实测复现，属待修复项。

### 1. 扩容容量计算错误

```java
int newCap = oldCap ^ 2;   // ^ 是异或，既不是乘方也不是乘 2
```

`16 ^ 2 = 18`，容量并没有翻倍，且不再保持 2 的幂；更麻烦的是 `18 ^ 2 = 16`，容量会在 16 与 18 之间来回震荡。

实测：连续写入 73 个元素后，`table.length` 仍然只有 **18**。

```java
int newCap = oldCap << 1;   // 正确写法，等价于 oldCap * 2
```

### 2. 下标可能为负导致数组越界

```java
int index = hash % table.length;   // hashCode 为负时，结果也为负
```

实测：`map.put(-1, "neg")` 抛出 `ArrayIndexOutOfBoundsException: Index -1 out of bounds for length 16`。

```java
int index = (hash & 0x7FFFFFFF) % table.length;
```

> 若把容量严格约束为 2 的幂，用 JDK 的 `hash & (table.length - 1)` 效率更高，但同样需要先处理符号位。

### 3. `getForNullKey()` 判断条件写反

```java
if (cur.key != null) {        // 应为 == null
    return cur.value;
}
```

实测：`put("a", 1); put(null, 42); get(null)` 返回 `null`，预期 `42`。当前逻辑会返回 0 号桶中第一个非 `null` key 的值；如果 `null` key 不在链头，则完全取不到。

### 4. `main` 方法签名不完整

`static void main()` 缺少 `String[] args` 参数。在 JDK 25 上可以运行，但在 JDK 17 / 21 上不是合法入口点。建议改为 `public static void main(String[] args)`，并把测试代码从 `SimpleHashMap` 中拆到独立的 `Main` 或单元测试类。

### 5. 功能缺失

`remove` / `containsKey` / `isEmpty` / `clear` / `keySet` / 遍历接口均未实现，也没有任何单元测试。

---

## TODO

- [ ] 修复上述第 1–3 项逻辑错误
- [ ] 补充 `remove`、`containsKey`、`isEmpty`、`clear`
- [ ] 用 JUnit 5 补单元测试，覆盖哈希冲突、扩容、`null` key、负数 `hashCode`
- [ ] 对齐 JDK 8 优化：链表长度 ≥ 8 时转红黑树
- [ ] 改用 `hash & (n - 1)` 取模，把容量严格约束为 2 的幂
- [ ] 实现线程安全版本，对比 `ConcurrentHashMap` 的 CAS / 分段思路

---

## 说明

个人学习练习项目，暂未指定开源协议。
