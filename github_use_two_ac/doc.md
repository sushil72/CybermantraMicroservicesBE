Yes, you can name them like that. A clean setup would be:

- **`devsushil`** = your current GitHub account/key
- **`sushil`** = your previous GitHub account/key

Your current key will remain:

```shell script
~/.ssh/id_ed25519
```


New key for previous account can be:

```shell script
~/.ssh/id_ed25519_sushil
```


## 1. Create new key for `sushil`

Run:

```shell script
ssh-keygen -t ed25519 -C "<sushil-github-email>" -f ~/.ssh/id_ed25519_sushil
```


This creates:

```shell script
~/.ssh/id_ed25519_sushil
~/.ssh/id_ed25519_sushil.pub
```


## 2. Add the new key to SSH agent

```shell script
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/id_ed25519_sushil
```


Also add your current key if needed:

```shell script
ssh-add ~/.ssh/id_ed25519
```


## 3. Add new public key to `sushil` GitHub account

Show the public key:

```shell script
cat ~/.ssh/id_ed25519_sushil.pub
```


Copy the output and add it to the `sushil` GitHub account:

```plain text
GitHub → Settings → SSH and GPG keys → New SSH key
```


## 4. Configure SSH aliases

Open SSH config:

```shell script
nano ~/.ssh/config
```


Add this:

```plain text
Host github-devsushil
    HostName github.com
    User git
    IdentityFile ~/.ssh/id_ed25519
    IdentitiesOnly yes

Host github-sushil
    HostName github.com
    User git
    IdentityFile ~/.ssh/id_ed25519_sushil
    IdentitiesOnly yes
```


Save with:

```plain text
Ctrl + O
Enter
Ctrl + X
```


## 5. Test both accounts

Test current account:

```shell script
ssh -T git@github-devsushil
```


Test previous account:

```shell script
ssh -T git@github-sushil
```


You should see:

```plain text
Hi <github-username>! You've successfully authenticated...
```


## 6. Use correct remote URL

For `devsushil` account repos:

```shell script
git remote set-url origin git@github-devsushil:<repo-owner>/<repo-name>.git
```


For `sushil` account repos:

```shell script
git remote set-url origin git@github-sushil:<repo-owner>/<repo-name>.git
```


Check:

```shell script
git remote -v
```


## Final naming summary

Current key/account:

```plain text
Alias: github-devsushil
Key: ~/.ssh/id_ed25519
```


Previous key/account:

```plain text
Alias: github-sushil
Key: ~/.ssh/id_ed25519_sushil
```


This will let you switch easily between both GitHub accounts without overwriting any key.
