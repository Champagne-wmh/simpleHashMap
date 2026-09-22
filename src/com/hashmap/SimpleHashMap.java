package com.hashmap;
public class SimpleHashMap<K, V> {
    private Node<K, V>[] table;
    private int size;
    private final float loadFactor = 0.75f;
    private int threshold;

    @SuppressWarnings("unchecked")
    public SimpleHashMap() {
        table = (Node<K, V>[]) new Node[16];
        threshold = (int) (table.length * loadFactor);
    }

    static class Node<K, V> {
        //节点静态内部类
        final K key;
        V value;
        Node<K, V> next;

        Node(K key, V value, Node<K, V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    public V put(K key, V value) {
        if (key == null) {
            return putForNullKey(value);
        }
        int hash = key.hashCode();
        // & 0x7FFFFFFF 抹掉符号位，否则 hashCode 为负时下标为负，会数组越界
        int index = (hash & 0x7FFFFFFF) % table.length;
        Node<K, V> cur = table[index];
        while (cur != null) {
            if (cur.key.hashCode() == hash && cur.key.equals(key)) {
                V oldVal = cur.value;
                cur.value = value;
                return oldVal;
            }
            cur = cur.next;
        }
        table[index] = new Node<>(key, value, table[index]);
        size++;
        if (size > threshold) {
            resize();
        }
        return null;
    }


    public V get(K key) {
        if(key == null){
            return getForNullKey();
        }
        int hash = key.hashCode();
        // 同 put：先抹掉符号位
        int index = (hash & 0x7FFFFFFF) % table.length;
        Node<K, V> cur = table[index];
        while(cur != null){
            if(cur.key.hashCode() == hash && cur.key.equals(key)){
                return cur.value;
            }
            cur = cur.next;
        }
        return null;
    }

    public V putForNullKey(V value) {
        // 处理key为null的情况
        Node<K, V> cur = table[0];
        while (cur != null) {
            if (cur.key == null) {
                V oldVal = cur.value;
                cur.value = value;
                return oldVal;
            }
            cur = cur.next;
        }
        table[0] = new Node<>(null, value, table[0]);
        size++;
        if (size > threshold) {
            resize();
        }
        return null;
    }

    public V getForNullKey(){
        Node<K,V> cur = table[0];
        while(cur != null){
            if(cur.key == null){
                return cur.value;
            }
            cur = cur.next;
        }
        return null;
    }


    @SuppressWarnings("unchecked")
    public void resize() {
        //扩容
        Node<K, V>[] oldTable = table;
        int oldCap = oldTable.length;
        int newCap = oldCap * 2;
        Node<K, V>[] newTable = (Node<K, V>[]) new Node[newCap];
        threshold = (int) (newCap * loadFactor);
        table = newTable;

        //迁移节点
        for (int i = 0; i < oldCap; i++) {
            Node<K,V> node = oldTable[i];
            if(node != null){
                oldTable[i] = null;
                while(node != null){
                    Node<K,V>next = node.next;
                    int newIndex = node.key == null ? 0 : (node.key.hashCode() & 0x7FFFFFFF) % newCap;
                    node.next = newTable[newIndex];
                    newTable[newIndex] = node;
                    node = next;
                }
            }
        }
    }

    public int size(){
        return size;
    }

    public static void main(String[] args) {
        SimpleHashMap<String, Integer> map = new SimpleHashMap<>();
        map.put("a",1);
        map.put("b",2);
        map.put("a",99);
        map.put(null,1);
        System.out.println(map.get("a"));
        System.out.println(map.get("b"));
        System.out.println(map.get(null));
        System.out.println(map.size());
    }
}

