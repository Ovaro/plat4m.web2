#!/bin/bash
cd ..
std_name=plt4m-0.0.1-SNAPSHOT.jar
# Check if we are inside a git repository
if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "Not inside a git repository."
  exit 1
fi

# Check if repository is dirty
if git diff-index --quiet HEAD --; then
  echo "Repository is clean. Skipping commit step."
else
  echo "Repository has uncommitted changes."

  # Prompt to commit changes
  read -rp "Do you want to commit these changes? (y/n): " commit_answer
  if [[ "$commit_answer" == "y" ]]; then
    
    # Ask for commit message
    read -rp "Enter commit message: " commit_message

    # Stage all changes and commit
    git add -A
    git commit -m "$commit_message"
    echo "Changes committed."

    latest_tag=$(git describe --tags --abbrev=0 2>/dev/null || echo "No tags found")
    echo "Latest tag: $latest_tag"

    # Prompt to create a tag
    read -rp "Do you want to create a tag for this commit? (y/n): " tag_answer
    if [[ "$tag_answer" == "y" ]]; then
      read -rp "Enter tag name: " tag_name
      git tag "$tag_name"
      echo "Tag '$tag_name' created."
    fi
  else
    echo "No commit made."
  fi
fi
new_name=$(git describe --tags 2>/dev/null)
echo ---------------------------------------------------
echo Building
echo ---------------------------------------------------
./gradlew -Pprod bootJar -x test
cp ./build/libs/plat-4-m-0.0.1-SNAPSHOT.jar ./build/libs/plat4m-$new_name.jar
echo ---------------------------------------------------
echo Uploading 
echo ---------------------------------------------------
scp ./build/libs/plat4m-$new_name.jar bitnami@3.24.110.213:~/plat4m/builds/plat4m-$new_name.jar
if [ $? -eq 0 ]; then
	echo "SCP command was successful."

	echo ---------------------------------------------------
	echo Replacing old build
	echo ---------------------------------------------------
	ssh bitnami@3.24.110.213 "cp ~/plat4m/builds/plat4m-$new_name.jar ~/plat4m/prod/bitnami-live.jar" 
	echo ---------------------------------------------------
	echo Restarting Service 
	echo ---------------------------------------------------
	ssh bitnami@3.24.110.213 "sudo systemctl stop plat4m.service;sudo systemctl start plat4m.service;sudo systemctl status plat4m.service"  
	sleep 15
	ssh bitnami@3.24.110.213 "sudo systemctl status plat4m.service"  
	sleep 15
	ssh bitnami@3.24.110.213 "sudo systemctl status plat4m.service"  
	sleep 10
	ssh bitnami@3.24.110.213 "sudo systemctl status plat4m.service"  
else
	echo "SCP command failed. Exit code: $?"
fi
