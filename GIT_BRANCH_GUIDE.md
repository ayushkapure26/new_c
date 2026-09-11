# Creating and Managing Branches in Git

Git branches allow you to work on features independently and organize your development workflow. Here's a comprehensive guide with examples:

## 1. Creating Branches

### View existing branches
```bash
git branch                    # List local branches
git branch -a                 # List all branches (local + remote)
git branch -r                 # List remote branches only
```

### Create a new branch
```bash
git branch feature-login      # Create branch locally
git branch -b feature-login   # Alternative syntax
```

### Create and switch to a new branch in one command
```bash
git checkout -b feature-login           # Older syntax
git switch -c feature-login             # Modern syntax (Git 2.23+)
```

**Example:**
```bash
git checkout -b develop-authentication
# Creates and switches to the 'develop-authentication' branch
```

## 2. Switching Between Branches

### Using checkout
```bash
git checkout main             # Switch to main branch
git checkout feature-login    # Switch to feature branch
```

### Using switch (modern approach)
```bash
git switch main               # Switch to main branch
git switch feature-login      # Switch to feature branch
```

**Example:**
```bash
git switch feature-login
# Switched to branch 'feature-login'
```

## 3. Deleting Branches

```bash
git branch -d feature-login         # Delete local branch (safe)
git branch -D feature-login         # Force delete local branch
git push origin --delete feature-login  # Delete remote branch
```

**Example:**
```bash
git branch -d completed-feature
# Deleted branch completed-feature (was abc1234).
```

## 4. Merging Branches

Merge combines changes from one branch into another.

### Basic merge
```bash
git checkout main                   # Switch to target branch
git merge feature-login             # Merge feature branch into main
```

**Example - Fast-forward merge:**
```bash
git checkout main
git merge feature-login
# Fast-forward
#  feature-login.txt | 1 +
#  1 file changed, 1 insertion(+)
```

### Merge strategies

```bash
git merge --no-ff feature-login     # Create merge commit (preserve history)
git merge --squash feature-login    # Combine all commits into one
git merge --ff-only feature-login   # Only allow fast-forward merge
```

**Example - Squash merge:**
```bash
git merge --squash feature-auth
git commit -m "Add authentication module"
# Combines all commits from feature-auth into a single commit
```

## 5. Rebasing Branches

Rebase replays commits from one branch onto another, creating a linear history.

### Basic rebase
```bash
git checkout feature-login          # Switch to feature branch
git rebase main                     # Rebase onto main
```

**Example:**
```bash
# Before rebase:
# main:     A -- B -- C
# feature:         D -- E

git checkout feature-login
git rebase main

# After rebase:
# main:     A -- B -- C
# feature:              D' -- E'
```

### Interactive rebase (modify commit history)
```bash
git rebase -i HEAD~3                # Rebase last 3 commits
git rebase -i main                  # Rebase all commits since main
```

**Example - Interactive rebase options:**
```bash
# In the interactive editor:
pick abc1234 Fix login bug
reword def5678 Add password validation
squash ghi9012 Update tests

# Commands:
# pick   = use commit
# reword = use commit, but edit message
# squash = use commit, but meld into previous
# fixup  = like squash, but discard log message
# drop   = remove commit
```

### Continuing after rebase conflicts
```bash
git rebase --continue              # Continue after resolving conflicts
git rebase --abort                 # Abort the rebase operation
git rebase --skip                  # Skip the current commit
```

**Example:**
```bash
# If conflicts occur during rebase:
git rebase main
# CONFLICT (content): Merge conflict in login.js

# Resolve conflicts manually in your editor, then:
git add login.js
git rebase --continue
```

## 6. Common Workflows

### Feature Branch Workflow
```bash
# Create and work on feature
git checkout -b feature/user-profile
# Make changes and commits
git add .
git commit -m "Add user profile page"

# Switch to main and merge
git checkout main
git pull origin main
git merge feature/user-profile
git push origin main

# Delete feature branch
git branch -d feature/user-profile
git push origin --delete feature/user-profile
```

### Rebasing vs. Merging (When to use each)

**Use Merge when:**
- Integrating a completed feature into main
- You want to preserve branch history
- Working in a team with shared branches

**Use Rebase when:**
- Updating your feature branch with latest main changes
- Cleaning up local commit history before merging
- Working on a personal feature branch

## 7. Useful Branch Management Commands

```bash
git branch -v                       # Show branches with last commit
git branch -vv                      # Show tracking relationships
git branch --merged                 # Show merged branches
git branch --no-merged              # Show unmerged branches
git branch -m old-name new-name     # Rename a branch
git branch -m new-name              # Rename current branch
```

**Example:**
```bash
git branch -v
# * main          abc1234 Update README
#   feature-auth  def5678 Add authentication
#   feature-ui    ghi9012 Redesign dashboard
```

---

These commands form the foundation of effective Git branch management. Practice these workflows to improve your development process!