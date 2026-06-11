import React, { useState, useEffect } from 'react';
import './index.css';

interface Customer {
  customerId: string;
  fullName: string;
  email: string;
  phone: string;
  address: string;
}

interface Account {
  accountNumber: string;
  accountType: string;
  balance: number;
  status: string;
}

interface Transaction {
  transactionId: string;
  transactionType: string;
  amount: number;
  balanceAfter: number;
  transactionDate: string;
  description: string;
}

type ViewState = 'LOGIN' | 'SIGNUP' | 'DASHBOARD' | 'CREATE_ACCOUNT';

function App() {
  const [view, setView] = useState<ViewState>('LOGIN');
  
  // State
  const [loggedInCustomer, setLoggedInCustomer] = useState<Customer | null>(null);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [selectedAccount, setSelectedAccount] = useState<Account | null>(null);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  
  // Forms
  const [loginId, setLoginId] = useState('');
  
  const [signupForm, setSignupForm] = useState({ fullName: '', email: '', phone: '', address: '' });
  const [newAccountForm, setNewAccountForm] = useState({ accountType: 'SAVINGS', initialBalance: '', pinHash: '0000' });
  const [transactionForm, setTransactionForm] = useState({ amount: '', description: '', pin: '' });

  const API_BASE = `http://${window.location.hostname}:8080/api`;

  // --- API Calls ---

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await fetch(`${API_BASE}/customers/${loginId}`);
      if (!res.ok) throw new Error('Customer not found');
      const data = await res.json();
      setLoggedInCustomer(data);
      fetchAccounts(data.customerId);
      setView('DASHBOARD');
    } catch (err) {
      alert('Invalid Customer ID. Please try CUST000001 or Sign Up.');
    }
  };

  const handleSignup = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await fetch(`${API_BASE}/customers`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(signupForm)
      });
      const data = await res.json();
      alert(`Signup successful! Your Customer ID is: ${data.customerId}\nPlease save this to login.`);
      setLoggedInCustomer(data);
      setAccounts([]);
      setView('DASHBOARD');
    } catch (err) {
      alert('Signup failed. Ensure email/phone are unique.');
    }
  };

  const fetchAccounts = async (customerId: string) => {
    try {
      const res = await fetch(`${API_BASE}/customers/${customerId}/accounts`);
      const data = await res.json();
      setAccounts(data);
    } catch (err) {
      console.error(err);
    }
  };

  const handleCreateAccount = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!loggedInCustomer) return;
    try {
      await fetch(`${API_BASE}/customers/${loggedInCustomer.customerId}/accounts`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(newAccountForm)
      });
      alert('Account created successfully!');
      fetchAccounts(loggedInCustomer.customerId);
      setView('DASHBOARD');
      setNewAccountForm({ accountType: 'SAVINGS', initialBalance: '', pinHash: '0000' });
    } catch (err) {
      alert('Failed to create account.');
    }
  };

  const fetchTransactions = async (accountNum: string) => {
    try {
      const res = await fetch(`${API_BASE}/accounts/${accountNum}/transactions`);
      const data = await res.json();
      setTransactions(data);
    } catch (err) {
      console.error(err);
    }
  };

  const handleSelectAccount = (acc: Account) => {
    setSelectedAccount(acc);
    fetchTransactions(acc.accountNumber);
  };

  const handleTransaction = async (type: string) => {
    if (!selectedAccount || !transactionForm.amount || !transactionForm.pin) return;
    try {
      const res = await fetch(`${API_BASE}/accounts/${selectedAccount.accountNumber}/transactions`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          amount: transactionForm.amount,
          transactionType: type,
          description: transactionForm.description || `${type} transaction`,
          pin: transactionForm.pin,
        }),
      });
      
      if (!res.ok) {
        throw new Error('Transaction failed or Invalid PIN');
      }

      // Refresh
      const accRes = await fetch(`${API_BASE}/accounts/${selectedAccount.accountNumber}`);
      const updatedAccount = await accRes.json();
      setSelectedAccount(updatedAccount);
      setAccounts(accounts.map(a => a.accountNumber === updatedAccount.accountNumber ? updatedAccount : a));
      
      fetchTransactions(selectedAccount.accountNumber);
      setTransactionForm({ amount: '', description: '', pin: '' });
      alert(`${type} successful!`);
    } catch (err) {
      alert('Transaction failed. Check your balance or PIN.');
    }
  };

  const logout = () => {
    setLoggedInCustomer(null);
    setSelectedAccount(null);
    setAccounts([]);
    setLoginId('');
    setView('LOGIN');
  };

  // --- Views ---

  return (
    <>
      <header className="header glass" style={{ marginBottom: '2rem' }}>
        <h1>NexusBank Portal</h1>
        {loggedInCustomer && (
          <div style={{ display: 'flex', gap: '1rem', alignItems: 'center' }}>
            <span>Welcome, <strong>{loggedInCustomer.fullName}</strong></span>
            <button className="btn btn-danger" onClick={logout}>Logout</button>
          </div>
        )}
      </header>

      <main className="container animate-fade-in">
        
        {/* LOGIN VIEW */}
        {view === 'LOGIN' && (
          <div className="card glass" style={{ maxWidth: '400px', margin: '0 auto' }}>
            <h2>Secure Login</h2>
            <form onSubmit={handleLogin}>
              <div className="form-group">
                <label>Customer ID</label>
                <input 
                  type="text" 
                  className="form-control" 
                  value={loginId} 
                  onChange={e => setLoginId(e.target.value)} 
                  placeholder="e.g., CUST000001" 
                  required 
                />
              </div>
              <button type="submit" className="btn" style={{ width: '100%', marginBottom: '1rem' }}>Login</button>
            </form>
            <div style={{ textAlign: 'center', marginTop: '1rem' }}>
              <span style={{ color: 'var(--text-secondary)' }}>New to NexusBank? </span>
              <button className="btn btn-success" onClick={() => setView('SIGNUP')} style={{ padding: '0.4rem 1rem' }}>Sign Up</button>
            </div>
          </div>
        )}

        {/* SIGNUP VIEW */}
        {view === 'SIGNUP' && (
          <div className="card glass" style={{ maxWidth: '500px', margin: '0 auto' }}>
            <h2>Create an Account</h2>
            <form onSubmit={handleSignup}>
              <div className="form-group">
                <label>Full Name</label>
                <input type="text" className="form-control" required value={signupForm.fullName} onChange={e => setSignupForm({...signupForm, fullName: e.target.value})} />
              </div>
              <div className="form-group">
                <label>Email Address</label>
                <input type="email" className="form-control" required value={signupForm.email} onChange={e => setSignupForm({...signupForm, email: e.target.value})} />
              </div>
              <div className="form-group">
                <label>Phone Number</label>
                <input type="text" className="form-control" required value={signupForm.phone} onChange={e => setSignupForm({...signupForm, phone: e.target.value})} />
              </div>
              <div className="form-group">
                <label>Address</label>
                <input type="text" className="form-control" required value={signupForm.address} onChange={e => setSignupForm({...signupForm, address: e.target.value})} />
              </div>
              <div style={{ display: 'flex', gap: '1rem' }}>
                <button type="button" className="btn btn-danger" onClick={() => setView('LOGIN')} style={{ flex: 1 }}>Cancel</button>
                <button type="submit" className="btn btn-success" style={{ flex: 1 }}>Register</button>
              </div>
            </form>
          </div>
        )}

        {/* CREATE ACCOUNT VIEW */}
        {view === 'CREATE_ACCOUNT' && (
          <div className="card glass" style={{ maxWidth: '500px', margin: '0 auto' }}>
            <h2>Open New Bank Account</h2>
            <form onSubmit={handleCreateAccount}>
              <div className="form-group">
                <label>Account Type</label>
                <select className="form-control" value={newAccountForm.accountType} onChange={e => setNewAccountForm({...newAccountForm, accountType: e.target.value})}>
                  <option value="SAVINGS">Savings Account</option>
                  <option value="CURRENT">Current Account</option>
                </select>
              </div>
              <div className="form-group">
                <label>Initial Deposit ($)</label>
                <input type="number" className="form-control" required min="0" value={newAccountForm.initialBalance} onChange={e => setNewAccountForm({...newAccountForm, initialBalance: e.target.value})} />
              </div>
              <div className="form-group">
                <label>4-Digit PIN</label>
                <input type="password" className="form-control" required maxLength={4} minLength={4} value={newAccountForm.pinHash} onChange={e => setNewAccountForm({...newAccountForm, pinHash: e.target.value})} />
              </div>
              <div style={{ display: 'flex', gap: '1rem' }}>
                <button type="button" className="btn btn-danger" onClick={() => setView('DASHBOARD')} style={{ flex: 1 }}>Cancel</button>
                <button type="submit" className="btn btn-success" style={{ flex: 1 }}>Open Account</button>
              </div>
            </form>
          </div>
        )}

        {/* DASHBOARD VIEW */}
        {view === 'DASHBOARD' && loggedInCustomer && (
          <div className="animate-fade-in">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
              <h2>Your Dashboard</h2>
              <button className="btn btn-success" onClick={() => setView('CREATE_ACCOUNT')}>+ Open New Account</button>
            </div>

            <div className="card glass" style={{ marginBottom: '2rem', display: 'flex', flexWrap: 'wrap', gap: '2rem' }}>
              <div>
                <p style={{ color: 'var(--text-secondary)', margin: 0, fontSize: '0.9rem' }}>Customer ID</p>
                <p style={{ fontWeight: 'bold', fontSize: '1.2rem', margin: '0.2rem 0' }}>{loggedInCustomer.customerId}</p>
              </div>
              <div>
                <p style={{ color: 'var(--text-secondary)', margin: 0, fontSize: '0.9rem' }}>Email</p>
                <p style={{ margin: '0.2rem 0' }}>{loggedInCustomer.email}</p>
              </div>
              <div>
                <p style={{ color: 'var(--text-secondary)', margin: 0, fontSize: '0.9rem' }}>Phone</p>
                <p style={{ margin: '0.2rem 0' }}>{loggedInCustomer.phone}</p>
              </div>
              <div>
                <p style={{ color: 'var(--text-secondary)', margin: 0, fontSize: '0.9rem' }}>Address</p>
                <p style={{ margin: '0.2rem 0' }}>{loggedInCustomer.address}</p>
              </div>
            </div>
            
            <div className="dashboard-grid">
              <div className="card glass" style={{ minHeight: '300px' }}>
                {accounts.length === 0 ? (
                  <p style={{ color: 'var(--text-secondary)', textAlign: 'center', marginTop: '3rem' }}>You don't have any accounts yet. Open one to get started!</p>
                ) : (
                  accounts.map(a => (
                    <div 
                      key={a.accountNumber} 
                      onClick={() => handleSelectAccount(a)}
                      style={{
                        padding: '1.5rem', 
                        marginBottom: '1rem', 
                        backgroundColor: selectedAccount?.accountNumber === a.accountNumber ? 'rgba(99, 102, 241, 0.2)' : 'rgba(0,0,0,0.2)',
                        borderRadius: '12px',
                        cursor: 'pointer',
                        border: selectedAccount?.accountNumber === a.accountNumber ? '1px solid var(--primary)' : '1px solid transparent',
                        transition: 'all 0.2s'
                      }}
                    >
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                        <strong style={{ fontSize: '1.2rem' }}>{a.accountNumber}</strong>
                        <span className="status-positive" style={{ fontSize: '1.2rem', fontWeight: 'bold' }}>${a.balance.toLocaleString()}</span>
                      </div>
                      <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-secondary)' }}>
                        <small>{a.accountType}</small>
                        <small>Status: {a.status}</small>
                      </div>
                    </div>
                  ))
                )}
              </div>

              {selectedAccount && (
                <div className="card glass">
                  <h2>Transfer Funds</h2>
                  <p style={{ color: 'var(--text-secondary)', marginBottom: '1.5rem' }}>Selected: <strong>{selectedAccount.accountNumber}</strong></p>
                  
                  <div className="form-group">
                    <label>Amount ($)</label>
                    <input 
                      type="number" 
                      className="form-control" 
                      value={transactionForm.amount} 
                      onChange={e => setTransactionForm({...transactionForm, amount: e.target.value})} 
                      placeholder="0.00"
                    />
                  </div>
                  <div className="form-group">
                    <label>Description (Optional)</label>
                    <input 
                      type="text" 
                      className="form-control" 
                      value={transactionForm.description} 
                      onChange={e => setTransactionForm({...transactionForm, description: e.target.value})} 
                      placeholder="e.g. Salary, Rent"
                    />
                  </div>
                  <div className="form-group">
                    <label>Enter 4-Digit PIN to Confirm</label>
                    <input 
                      type="password" 
                      className="form-control" 
                      maxLength={4}
                      minLength={4}
                      required
                      value={transactionForm.pin} 
                      onChange={e => setTransactionForm({...transactionForm, pin: e.target.value})} 
                      placeholder="****"
                    />
                  </div>
                  <div style={{ display: 'flex', gap: '1rem', marginTop: '2rem' }}>
                    <button className="btn btn-success" style={{ flex: 1, padding: '1rem' }} onClick={() => handleTransaction('DEPOSIT')}>Deposit (+)</button>
                    <button className="btn btn-danger" style={{ flex: 1, padding: '1rem' }} onClick={() => handleTransaction('WITHDRAWAL')}>Withdraw (-)</button>
                  </div>
                </div>
              )}
            </div>

            {selectedAccount && (
              <div className="table-container glass card" style={{ marginTop: '2rem' }}>
                <h2>Transaction History</h2>
                <table>
                  <thead>
                    <tr>
                      <th>Date</th>
                      <th>Type</th>
                      <th>Description</th>
                      <th>Amount</th>
                      <th>Remaining Balance</th>
                    </tr>
                  </thead>
                  <tbody>
                    {transactions.map(t => (
                      <tr key={t.transactionId}>
                        <td>{new Date(t.transactionDate).toLocaleString()}</td>
                        <td>
                          <span style={{ 
                            padding: '4px 8px', 
                            borderRadius: '4px', 
                            backgroundColor: t.transactionType.includes('IN') || t.transactionType === 'DEPOSIT' ? 'rgba(16, 185, 129, 0.2)' : 'rgba(239, 68, 68, 0.2)',
                            fontSize: '0.8rem',
                            fontWeight: 'bold'
                          }}>
                            {t.transactionType}
                          </span>
                        </td>
                        <td>{t.description || '-'}</td>
                        <td className={t.transactionType.includes('IN') || t.transactionType === 'DEPOSIT' ? 'status-positive' : 'status-negative'}>
                          {t.transactionType.includes('IN') || t.transactionType === 'DEPOSIT' ? '+' : '-'}${t.amount.toLocaleString()}
                        </td>
                        <td>${t.balanceAfter.toLocaleString()}</td>
                      </tr>
                    ))}
                    {transactions.length === 0 && (
                      <tr>
                        <td colSpan={5} style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-secondary)' }}>
                          No transactions found for this account.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

      </main>
    </>
  );
}

export default App;
