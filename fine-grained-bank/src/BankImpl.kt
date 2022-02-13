import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Bank implementation.
 *
 * @author : Slastin Aleksandr
 */
class BankImpl(n: Int) : Bank {
    private val accounts: Array<Account> = Array(n) { Account() }

    override val numberOfAccounts: Int
        get() = accounts.size

    override fun getAmount(index: Int): Long = accounts[index].lock.withLock { accounts[index].amount }

    override val totalAmount: Long
        get() {
            accounts.forEach { it.lock.lock() }
            val res = accounts.sumOf { account -> account.amount }
            accounts.forEach { it.lock.unlock() }
            return res
        }

    override fun deposit(index: Int, amount: Long): Long {
        require(amount > 0) { "Invalid amount: $amount" }
        check(amount <= Bank.MAX_AMOUNT) { "Overflow" }
        val account = accounts[index]
        account.lock.withLock {
            check(account.amount + amount <= Bank.MAX_AMOUNT) { "Overflow" }
            account.amount += amount
            return account.amount
        }
    }

    override fun withdraw(index: Int, amount: Long): Long {
        require(amount > 0) { "Invalid amount: $amount" }
        val account = accounts[index]
        account.lock.withLock {
            check(account.amount - amount >= 0) { "Underflow" }
            account.amount -= amount
            return account.amount
        }
    }

    override fun transfer(fromIndex: Int, toIndex: Int, amount: Long) {
        require(amount > 0) { "Invalid amount: $amount" }
        require(fromIndex != toIndex) { "fromIndex == toIndex" }
        check(amount <= Bank.MAX_AMOUNT) { "Overflow" }
        val from = accounts[fromIndex]
        val to = accounts[toIndex]
        val (lock1, lock2) = if (fromIndex < toIndex) Pair(from.lock, to.lock) else Pair(to.lock, from.lock)
        lock1.withLock {
            lock2.withLock {
                check(amount <= from.amount) { "Underflow" }
                check(to.amount + amount <= Bank.MAX_AMOUNT) { "Overflow" }
                from.amount -= amount
                to.amount += amount
            }
        }
    }

    /**
     * Private account data structure.
     */
    class Account {
        /**
         * Amount of funds in this account.
         */
        var amount: Long = 0

        val lock = ReentrantLock()
    }
}
