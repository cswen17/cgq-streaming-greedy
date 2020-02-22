#!/bin/bash
JAR_PATH=$1
USER_NAME=$2

CURRENT_WORKING_DIR=$(pwd)

ls | grep app

if [[ $? != 0 ]]; then
  echo "Cannot run script from $CURRENT_WORKING_DIR"
  exit 1;
fi;

ls app/user_uploaded/intermediary_holding | grep $JAR_PATH

if [[ $? != 0 ]]; then
  echo "Cannot find $JAR_PATH in $CURRENT_WORKING_DIR/app/user_uploaded/intermediary_holding"
  exit 1
fi;

mkdir -p app/user_uploaded/$USER_NAME

cp -r app/user_uploaded/intermediary_holding/$JAR_PATH app/user_uploaded/$USER_NAME

cd app/user_uploaded/$USER_NAME

jar xf $JAR_PATH

rm $JAR_PATH

ls -R . | grep scala

if [[ $? != 0 ]]; then
    echo "Cannot find any .scala files in $JAR_PATH"
    exit 1
fi;

ls -R . | grep class

if [[ $? == 0 ]]; then
    echo "Warning: .class files found in $JAR_PATH"
fi;

ls | grep definitions.json

if [[ $? != 0 ]]; then
    echo "User did not include definitions.json in the right place"
    exit 1
fi;

# Sample definitions.json
# ~~~~~~~~~~~~~~~~~~~~~~~~~
# {
#   "ground_set_api": {
#     "name": "NodesGround",
#     "object_import_path": "NodesGround._",
#     "package": "graph",
#     "filename": "graph/NodesGround.scala",
#     "serial_id": "NodesGround"
#   },
#   "independence_api": {
#     "name": "NodesIndependence",
#     "object_import_path": "NodesIndependence._",
#     "package": "graph",
#     "filename": "graph/NodesIndependence.scala",
#     "serial_id": "NodesIndependence"
#   },
#   "marginal_value_api": {
#     "name": "LogMarginalValue",
#     "object_import_path": "LogMarginalValue._",
#     "package": "graph",
#     "filename": "graph/LogMarginalValue.scala",
#     "serial_id": "LogMarginalValue"
#   },
#   "label_iterator_api": {
#     "name": "VertexLabelIterator",
#     "object_import_path": "VertexLabelIterator._",
#     "package": "graph",
#     "filename": "graph/VertexLabelIterator.scala",
#     "serial_id": "VertexLabelIterator"
#   },
#   "element_companion": {
#     "name": "VertexSet",
#     "object_import_path": "._",
#     "package": "graph",
#     "filename": "graph/package.scala",
#     "serial_id": "VertexSet"
#   }
# }

GROUND_SET_API_PACKAGE_PATH=$(cat definitions.json | jq -r .ground_set_api.package)
GROUND_SET_API_IMPORT=$(cat definitions.json | jq -r .ground_set_api.object_import_path)
GROUND_SET_API_INVOCATION=$(cat definitions.json | jq -r .ground_set_api.name)
GROUND_SET_API_FILENAME=$(cat definitions.json | jq -r .ground_set_api.filename)
GROUND_SET_API_SERIAL_ID=$(cat definitions.json | jq -r .ground_set_api.serial_id)

INDEPENDENCE_API_PACKAGE_PATH=$(cat definitions.json | jq -r .independence_api.package)
INDEPENDENCE_API_IMPORT=$(cat definitions.json | jq -r .independence_api.object_import_path)
INDEPENDENCE_API_INVOCATION=$(cat definitions.json | jq -r .independence_api.name)
INDEPENDENCE_API_FILENAME=$(cat definitions.json | jq -r .independence_api.filename)
INDEPENDENCE_API_SERIAL_ID=$(cat definitions.json | jq -r .independence_api.serial_id)

MARGINAL_VALUE_API_PACKAGE_PATH=$(cat definitions.json | jq -r .marginal_value_api.package)
MARGINAL_VALUE_API_IMPORT=$(cat definitions.json | jq -r .marginal_value_api.object_import_path)
MARGINAL_VALUE_API_INVOCATION=$(cat definitions.json | jq -r .marginal_value_api.name)
MARGINAL_VALUE_API_FILENAME=$(cat definitions.json | jq -r .marginal_value_api.filename)
MARGINAL_VALUE_API_SERIAL_ID=$(cat definitions.json | jq -r .marginal_value_api.serial_id)

LABEL_ITERATOR_API_PACKAGE_PATH=$(cat definitions.json | jq -r .label_iterator_api.package)
LABEL_ITERATOR_API_IMPORT=$(cat definitions.json | jq -r .label_iterator_api.object_import_path)
LABEL_ITERATOR_API_INVOCATION=$(cat definitions.json | jq -r .label_iterator_api.name)
LABEL_ITERATOR_API_FILENAME=$(cat definitions.json | jq -r .label_iterator_api.filename)
LABEL_ITERATOR_API_SERIAL_ID=$(cat definitions.json | jq -r .label_iterator_api.serial_id)

ELEMENT_COMPANION_PACKAGE_PATH=$(cat definitions.json | jq -r element_companion.package)
ELEMENT_COMPANION_IMPORT=$(cat definitions.json | jq -r .element_companion.object_import_path)
ELEMENT_COMPANION_INVOCATION=$(cat definitions.json | jq -r .element_companion.name)
ELEMENT_COMPANION_FILENAME=$(cat definitions.json | jq -r .element_companion.filename)
ELEMENT_COMPANION_SERIAL_ID=$(cat definitions.json | jq -r .element_companion.serial_id)

# begin sed setup
cd ..
ls | grep intermediary_holding
if [[ $? != 0 ]]; then
    echo "Could not successfully cd into app/user_uploaded: $(pwd)"
    exit 1;
fi

# sed the new package path into the filename
# this should come from a command file
echo "package user_uploaded.$USER_NAME.$GROUND_SET_API_PACKAGE_PATH" > intermediary_holding/package_names/ground_set.name
echo "package user_uploaded.$USER_NAME.$INDEPENDENCE_API_PACKAGE_PATH" > intermediary_holding/package_names/independence.name
echo "package user_uploaded.$USER_NAME.$MARGINAL_VALUE_API_PACKAGE_PATH" > intermediary_holding/package_names/marginal_value.name
echo "package user_uploaded.$USER_NAME.$LABEL_ITERATOR_API_PACKAGE_PATH" > intermediary_holding/package_names/label_iterator.name

# cat intermediary_holding/package_names/*

echo "1,1x" > intermediary_holding/sed_commands/package.ground_set
echo "2,2g" >> intermediary_holding/sed_commands/package.ground_set

echo "1,1x" > intermediary_holding/sed_commands/package.independence
echo "2,2g" >> intermediary_holding/sed_commands/package.independence

echo "1,1x" > intermediary_holding/sed_commands/package.marginal_value
echo "2,2g" >> intermediary_holding/sed_commands/package.marginal_value

echo "1,1x" > intermediary_holding/sed_commands/package.label_iterator
echo "2,2g" >> intermediary_holding/sed_commands/package.label_iterator

sed -i .bak -f intermediary_holding/sed_commands/package.ground_set intermediary_holding/package_names/ground_set.name $USER_NAME/$GROUND_SET_API_FILENAME
sed -i .bak -f intermediary_holding/sed_commands/package.independence intermediary_holding/package_names/independence.name $USER_NAME/$INDEPENDENCE_API_FILENAME
sed -i .bak -f intermediary_holding/sed_commands/package.marginal_value intermediary_holding/package_names/marginal_value.name $USER_NAME/$MARGINAL_VALUE_API_FILENAME
sed -i .bak -f intermediary_holding/sed_commands/package.label_iterator intermediary_holding/package_names/label_iterator.name $USER_NAME/$LABEL_ITERATOR_API_FILENAME

########### FIXME: this is a workaround :(
# converts the user uploaded package.scala to our newly formatted package path
echo "package user_uploaded.$USER_NAME.$GROUND_SET_API_PACKAGE_PATH" > /tmp/package.scala
cat $USER_NAME/$GROUND_SET_API_PACKAGE_PATH/package.scala >> /tmp/package.scala
cp /tmp/package.scala $USER_NAME/$GROUND_SET_API_PACKAGE_PATH/package.scala
sed -i .bak -e 2,2d $USER_NAME/$GROUND_SET_API_PACKAGE_PATH/package.scala
###########

# sed the new import path into the Factory
# the new import path should have the format `import user_uploaded.$USER_NAME.$GROUND_SET_API_IMPORT`
GROUND_SET_IMPORT=$(cat intermediary_holding/import_lines/ground_set.line)
INDEPENDENCE_IMPORT=$(cat intermediary_holding/import_lines/independence.line)
MARGINAL_VALUE_IMPORT=$(cat intermediary_holding/import_lines/marginal_value.line)
LABEL_ITERATOR_IMPORT=$(cat intermediary_holding/import_lines/label_iterator.line)
COMPANION_IMPORT=$(cat intermediary_holding/import_lines/element_companion.line)

echo "import user_uploaded.$USER_NAME.$GROUND_SET_API_IMPORT" > intermediary_holding/import_names/ground_set.name
echo "import user_uploaded.$USER_NAME.$INDEPENDENCE_API_IMPORT" > intermediary_holding/import_names/independence.name
echo "import user_uploaded.$USER_NAME.$MARGINAL_VALUE_API_IMPORT" > intermediary_holding/import_names/marginal_value.name
echo "import user_uploaded.$USER_NAME.$LABEL_ITERATOR_API_IMPORT" > intermediary_holding/import_names/label_iterator.name
echo "import user_uploaded.$USER_NAME.$ELEMENT_COMPANION_IMPORT" > intermediary_holding/import_names/element_companion.name

echo "1,1x" > intermediary_holding/sed_commands/import.ground_set
echo "$GROUND_SET_IMPORT,${GROUND_SET_IMPORT}G" >> intermediary_holding/sed_commands/import.ground_set

echo "1,1x" > intermediary_holding/sed_commands/import.independence
echo "$INDEPENDENCE_IMPORT,${INDEPENDENCE_IMPORT}G" >> intermediary_holding/sed_commands/import.independence

echo "1,1x" > intermediary_holding/sed_commands/import.marginal_value
echo "$MARGINAL_VALUE_IMPORT,${MARGINAL_VALUE_IMPORT}G" >> intermediary_holding/sed_commands/import.marginal_value

echo "1,1x" > intermediary_holding/sed_commands/import.marginal_value
echo "$LABEL_ITERATOR_IMPORT,${LABEL_ITERATOR_IMPORT}G" >> intermediary_holding/sed_commands/import.label_iterator

echo "1,1x" > intermediary_holding/sed_commands/import.marginal_value
echo "$ELEMENT_COMPANION_IMPORT,${ELEMENT_COMPANION_IMPORT}G" >> intermediary_holding/sed_commands/import.element_companion

sed -i .bak -f intermediary_holding/sed_commands/import.ground_set intermediary_holding/import_names/ground_set.name GroundSetAPIFactory.scala
sed -i .bak -f intermediary_holding/sed_commands/import.independence intermediary_holding/import_names/independence.name IndependenceAPIFactory.scala
sed -i .bak -f intermediary_holding/sed_commands/import.marginal_value intermediary_holding/import_names/marginal_value.name MarginalValueAPIFactory.scala
sed -i .bak -f intermediary_holding/sed_commands/import.label_iterator intermediary_holding/import_names/label_iterator.name LabelIteratorAPIFactory.scala
sed -i .bak -f intermediary_holding/sed_commands/import.element_companion intermediary_holding/import_names/element_companion.name ElementCompanionFactory.scala

# To Do: update intermediary_holding/apply_lines/*.line automatically

# sed the object invocation into the Factory
GROUND_SET_OBJECT=$(cat intermediary_holding/apply_lines/ground_set.line)
INDEPENDENCE_OBJECT=$(cat intermediary_holding/apply_lines/independence.line)
MARGINAL_VALUE_OBJECT=$(cat intermediary_holding/apply_lines/marginal_value.line)
LABEL_ITERATOR_OBJECT=$(cat intermediary_holding/apply_lines/label_iterator.line)
COMPANION_OBJECT=$(cat intermediary_holding/apply_lines/element_companion.line)

echo "        case \"${GROUND_SET_API_SERIAL_ID}\" => new ${GROUND_SET_API_INVOCATION}().asInstanceOf[GroundSetAPI[Element, L, GroundSetBase[Element]]]" > intermediary_holding/apply_names/ground_set.name
echo "        case \"${INDEPENDENCE_API_SERIAL_ID}\" => new ${INDEPENDENCE_API_INVOCATION}().asInstanceOf[IndependenceAPI[Element]]" > intermediary_holding/apply_names/independence.name
echo "        case \"${MARGINAL_VALUE_API_SERIAL_ID}\" => new ${MARGINAL_VALUE_API_INVOCATION}().asInstanceOf[MarginalValueAPI[Element]]" > intermediary_holding/apply_names/marginal_value.name
echo "        case \"${LABEL_ITERATOR_API_SERIAL_ID}\" => new ${LABEL_ITERATOR_API_INVOCATION}().asInstanceOf[IteratorAPI[L]]" > intermediary_holding/apply_names/label_iterator.name
echo "        case \"${ELEMENT_COMPANION_SERIAL_ID}\" => ${ELEMENT_COMPANION_INVOCATION}.asInstanceOf[ElementCompanion[Element]]" > intermediary_holding/apply_names/element_companion.name

echo "1,1x" > intermediary_holding/sed_commands/apply.ground_set
echo "$GROUND_SET_OBJECT,${GROUND_SET_OBJECT}G" >> intermediary_holding/sed_commands/apply.ground_set

echo "1,1x" > intermediary_holding/sed_commands/apply.independence
echo "$INDEPENDENCE_OBJECT,${INDEPENDENCE_OBJECT}G" >> intermediary_holding/sed_commands/apply.independence

echo "1,1x" > intermediary_holding/sed_commands/apply.marginal_value
echo "$MARGINAL_VALUE_OBJECT,${MARGINAL_VALUE_OBJECT}G" >> intermediary_holding/sed_commands/apply.marginal_value

echo "1,1x" > intermediary_holding/sed_commands/apply.marginal_value
echo "$LABEL_ITERATOR_OBJECT,${LABEL_ITERATOR_OBJECT}G" >> intermediary_holding/sed_commands/apply.label_iterator

echo "1,1x" > intermediary_holding/sed_commands/apply.marginal_value
echo "$COMPANION_OBJECT,${COMPANION_OBJECT}G" >> intermediary_holding/sed_commands/apply.element_companion

sed -i .bak -f intermediary_holding/sed_commands/apply.ground_set intermediary_holding/apply_names/ground_set.name GroundSetAPIFactory.scala
sed -i .bak -f intermediary_holding/sed_commands/apply.independence intermediary_holding/apply_names/independence.name IndependenceAPIFactory.scala
sed -i .bak -f intermediary_holding/sed_commands/apply.marginal_value intermediary_holding/apply_names/marginal_value.name MarginalValueAPIFactory.scala
sed -i .bak -f intermediary_holding/sed_commands/apply.label_iterator intermediary_holding/apply_names/label_iterator.name IteratorAPIFactory.scala
sed -i .bak -f intermediary_holding/sed_commands/apply.element_companion intermediary_holding/apply_names/element_companion.name ElementCompanionFactory.scala

###### FIXME: this is a workaround/afterthought
RAND_SUFF=$(jot -r 1 10000 99999)
CURR_TIME=$(date +%m-%d-%yT%Hh%Mm%s)
mkdir -p backups/$CURR_TIME/$RAND_SUFF
mv *.bak backups/$CURR_TIME/$RAND_SUFF
######

# To Do: update intermediary_holding/apply_lines/*.line automatically
APPLY_LINES=$(ls intermediary_holding/apply_lines)
for line in $APPLY_LINES; do
  OLD_LINE_NUMBER=$(cat intermediary_holding/apply_lines/$line)
  NEW_LINE_NUMBER=$(($OLD_LINE_NUMBER+2))
  echo "$NEW_LINE_NUMBER" > intermediary_holding/apply_lines/$line
  cat intermediary_holding/apply_lines/$line
done

# starting to run sbt for compilation
cd ../..
ls | grep app
if [[ $? != 0 ]]; then
    echo "Oops, couldn't find my way back to project root directory"
    exit 1;
fi

sbt compile

if [[ $? != 0 ]]; then
    echo "Project compilation failed"
    exit 1;
fi

# Random Port generation...
RANDOM_PORT=$(jot -r 1 2000 10000)

ps aux | grep "sbt-launch.jar run $RANDOM_PORT" | grep -v grep

if [[ $? == 0 ]]; then
    ANOTHER_RANDOM_PORT=$(jot -r 1 2000 10000)
    ps aux | grep "sbt-launch.jar run $ANOTHER_RANDOM_PORT" | grep -v grep
    if [[ $? != 0 ]]; then
        export RANDOM_PORT=$ANOTHER_RANDOM_PORT
    else
        echo "No port open. Tried $RANDOM_PORT and $ANOTHER_RANDOM_PORT"
        exit 1;
    fi;
fi;

sbt "run $RANDOM_PORT" &

# To Do: healthcheck
# To Do: graceful shutdown
