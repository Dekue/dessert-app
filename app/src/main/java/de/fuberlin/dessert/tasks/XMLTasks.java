/*******************************************************************************
 * Copyright 2010, Freie Universitaet Berlin (FUB). All rights reserved.
 * 
 * These sources were developed at the Freie Universitaet Berlin, 
 * Computer Systems and Telematics / Distributed, embedded Systems (DES) group 
 * (http://cst.mi.fu-berlin.de, http://www.des-testbed.net)
 * -------------------------------------------------------------------------------
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see http://www.gnu.org/licenses/ .
 * --------------------------------------------------------------------------------
 * For further information and questions please use the web site
 *        http://www.des-testbed.net
 ******************************************************************************/
package de.fuberlin.dessert.tasks;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import android.sax.Element;
import android.sax.ElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Log;
import android.util.Xml;
import de.fuberlin.dessert.Utils;
import de.fuberlin.dessert.model.config.ConfigEntryBoolean;
import de.fuberlin.dessert.model.config.ConfigEntryDecimal;
import de.fuberlin.dessert.model.config.ConfigEntryInteger;
import de.fuberlin.dessert.model.config.ConfigEntryList;
import de.fuberlin.dessert.model.config.ConfigEntrySpacer;
import de.fuberlin.dessert.model.config.ConfigEntryString;
import de.fuberlin.dessert.model.config.DaemonConfiguration;
import de.fuberlin.dessert.model.daemon.RepositoryDaemonInfo;
import de.fuberlin.dessert.model.manage.CommandLine;
import de.fuberlin.dessert.model.manage.CommandOption;
import de.fuberlin.dessert.model.manage.CommandOptionBoolean;
import de.fuberlin.dessert.model.manage.CommandOptionDecimal;
import de.fuberlin.dessert.model.manage.CommandOptionInteger;
import de.fuberlin.dessert.model.manage.CommandOptionList;
import de.fuberlin.dessert.model.manage.CommandOptionString;
import de.fuberlin.dessert.model.manage.ManageConfiguration;
import de.fuberlin.dessert.model.manage.ManageEntryCommand;
import de.fuberlin.dessert.model.manage.ManageEntryProperty;
import de.fuberlin.dessert.model.manage.ManageEntrySpacer;
import de.fuberlin.dessert.telnet.TelnetCommandMode;

/**
 * Contains the parsing logic and definition to read XML files from various
 * sources.
 * <p>
 * This is a collection of static methods.
 */
public class XMLTasks {

    private static final String LOG_TAG = "DESSERT -> XMLTasks";

    private static final String LAUNCH_ROOT_ELEMENT = "LaunchOptions";
    private static final String MANAGER_ROOT_ELEMENT = "ManagerOptions";
    private static final String REPOSITORY_ROOT_ELEMENT = "RepositoryIndex";

    private static final String SPACER_ELEMENT = "Spacer";
    private static final String STRING_ELEMENT = "String";
    private static final String INTEGER_ELEMENT = "Integer";
    private static final String DECIMAL_ELEMENT = "Decimal";
    private static final String BOOLEAN_ELEMENT = "Boolean";
    private static final String LIST_ELEMENT = "List";
    private static final String ITEM_ELEMENT = "Item";
    private static final String ENTRY_ELEMENT = "Entry";
    private static final String PROPERTY_ELEMENT = "Property";
    private static final String SETTER_COMMAND_ELEMENT = "SetterCommand";
    private static final String GETTER_COMMAND_ELEMENT = "GetterCommand";
    private static final String COMMAND_ELEMENT = "Command";
    private static final String COMMAND_LINE_ELEMENT = "CommandLine";
    private static final String LIST_OPTION_ELEMENT = "ListOption";
    private static final String DECIMAL_OPTION_ELEMENT = "DecimalOption";
    private static final String INTEGER_OPTION_ELEMENT = "IntegerOption";
    private static final String STRING_OPTION_ELEMENT = "StringOption";
    private static final String BOOLEAN_OPTION_ELEMENT = "BooleanOption";

    private static final String NAME_ATTRIBUTE = "name";
    private static final String DEFAULT_ATTRIBUTE = "default";
    private static final String DESCRIPTION_ATTRIBUTE = "description";
    private static final String MINVALUE_ATTRIBUTE = "minValue";
    private static final String MAXVALUE_ATTRIBUTE = "maxValue";
    private static final String TRUEVALUE_ATTRIBUTE = "trueValue";
    private static final String FALSEVALUE_ATTRIBUTE = "falseValue";
    private static final String VALUE_ATTRIBUTE = "value";
    private static final String VERSION_ATTRIBUTE = "version";
    private static final String APPLICATION_VERSION_ATTRIBUTE = "applicationVersion";
    private static final String LIBRARY_VERSION_ATTRIBUTE = "libraryVersion";
    private static final String PATH_ATTRIBUTE = "path";
    private static final String MODE_ATTRIBUTE = "mode";

    /**
     * Reads the configuration XML file from the given <code>configFile</code>.
     * 
     * @param configFile source file to parse
     * @return daemon configuration as described in <code>configFile</code>
     */
    public static DaemonConfiguration readConfigFile(File configFile) {
        final DaemonConfiguration result = new DaemonConfiguration();

        RootElement root = new RootElement(LAUNCH_ROOT_ELEMENT);

        root.getChild(SPACER_ELEMENT).setStartElementListener(new StartElementListener() {
            @Override
            public void start(Attributes attributes) {
                String description = attributes.getValue(DESCRIPTION_ATTRIBUTE);
                result.addEntry(new ConfigEntrySpacer(description));
            }
        });

        root.getChild(STRING_ELEMENT).setStartElementListener(new StartElementListener() {
            @Override
            public void start(Attributes attributes) {
                String name = attributes.getValue(NAME_ATTRIBUTE);
                String defaultValue = attributes.getValue(DEFAULT_ATTRIBUTE);
                String description = attributes.getValue(DESCRIPTION_ATTRIBUTE);
                result.addEntry(new ConfigEntryString(name, defaultValue, description));
            }
        });

        root.getChild(INTEGER_ELEMENT).setStartElementListener(new StartElementListener() {
            @Override
            public void start(Attributes attributes) {
                String name = attributes.getValue(NAME_ATTRIBUTE);
                String defaultValue = attributes.getValue(DEFAULT_ATTRIBUTE);
                String description = attributes.getValue(DESCRIPTION_ATTRIBUTE);
                String minValue = attributes.getValue(MINVALUE_ATTRIBUTE);
                String maxValue = attributes.getValue(MAXVALUE_ATTRIBUTE);
                result.addEntry(new ConfigEntryInteger(name, defaultValue, description, minValue, maxValue));
            }
        });

        root.getChild(DECIMAL_ELEMENT).setStartElementListener(new StartElementListener() {
            @Override
            public void start(Attributes attributes) {
                String name = attributes.getValue(NAME_ATTRIBUTE);
                String defaultValue = attributes.getValue(DEFAULT_ATTRIBUTE);
                String description = attributes.getValue(DESCRIPTION_ATTRIBUTE);
                String minValue = attributes.getValue(MINVALUE_ATTRIBUTE);
                String maxValue = attributes.getValue(MAXVALUE_ATTRIBUTE);
                result.addEntry(new ConfigEntryDecimal(name, defaultValue, description, minValue, maxValue));
            }
        });

        root.getChild(BOOLEAN_ELEMENT).setStartElementListener(new StartElementListener() {
            @Override
            public void start(Attributes attributes) {
                String name = attributes.getValue(NAME_ATTRIBUTE);
                String defaultValue = attributes.getValue(DEFAULT_ATTRIBUTE);
                String description = attributes.getValue(DESCRIPTION_ATTRIBUTE);
                String trueValue = attributes.getValue(TRUEVALUE_ATTRIBUTE);
                String falseValue = attributes.getValue(FALSEVALUE_ATTRIBUTE);
                result.addEntry(new ConfigEntryBoolean(name, defaultValue, description, trueValue, falseValue));
            }
        });

        final List<String> listItems = new ArrayList<>();
        Element listElement = root.getChild(LIST_ELEMENT);
        listElement.setElementListener(new ElementListener() {
            private String name;
            private String defaultValue;
            private String description;

            @Override
            public void end() {
                result.addEntry(new ConfigEntryList(name, defaultValue, description, listItems.toArray(new String[listItems.size()])));
            }

            @Override
            public void start(Attributes attributes) {
                name = attributes.getValue(NAME_ATTRIBUTE);
                defaultValue = attributes.getValue(DEFAULT_ATTRIBUTE);
                description = attributes.getValue(DESCRIPTION_ATTRIBUTE);
                listItems.clear();
            }
        });
        listElement.requireChild(ITEM_ELEMENT).setStartElementListener(new StartElementListener() {
            @Override
            public void start(Attributes attributes) {
                listItems.add(attributes.getValue(VALUE_ATTRIBUTE));
            }
        });

        FileReader reader = null;
        try {
            reader = new FileReader(configFile);
            Xml.parse(reader, root.getContentHandler());
        } catch (IOException | SAXException e) {
            Log.e(LOG_TAG, "Could not read configuration file: " + configFile.getAbsolutePath(), e);
        } finally {
            Utils.safelyClose(reader);
        }

        return result;
    }

    /**
     * Reads the management configuration of a daemon from the
     * <code>manageFile</code>.
     * 
     * @param manageFile source file to parse
     * @return configuration as described in the <code>manageFile</code>
     */
    public static ManageConfiguration readManageFile(File manageFile) {
        final ManageConfiguration result = new ManageConfiguration();

        // to hold the common inner items
        final List<CommandLine> commandLines = new ArrayList<>();
        final List<CommandOption> commandOptions = new ArrayList<>();

        // ROOT !!!!!!!!!!!!!
        final RootElement root = new RootElement(MANAGER_ROOT_ELEMENT);

        // SPACER !!!!!!!!!!!!!
        {
            root.getChild(SPACER_ELEMENT).setStartElementListener(new StartElementListener() {
                @Override
                public void start(Attributes attributes) {
                    String description = attributes.getValue(DESCRIPTION_ATTRIBUTE);
                    result.addEntry(new ManageEntrySpacer(description));
                }
            });
        }

        // PROPERTY !!!!!!!!!!!!!
        {
            final CommandLine[] getterCommandLineHolder = new CommandLine[1];
            Element propertyElement = root.getChild(PROPERTY_ELEMENT);
            propertyElement.setElementListener(new ElementListener() {
                private String description;

                @Override
                public void end() {
                    result.addEntry(new ManageEntryProperty(description,
                            getterCommandLineHolder[0],
                            commandLines.toArray(new CommandLine[commandLines.size()]),
                            commandOptions.toArray(new CommandOption[commandOptions.size()])));
                }

                @Override
                public void start(Attributes attributes) {
                    description = attributes.getValue(DESCRIPTION_ATTRIBUTE);
                    getterCommandLineHolder[0] = null;
                    commandLines.clear();
                    commandOptions.clear();
                }
            });

            // PROPERTY CHILDREN !!!!!!!!!!!!!
            {
                Element getterCommandElement = propertyElement.requireChild(GETTER_COMMAND_ELEMENT);

                // GETTER CHILDREN !!!!!!!!!!!!!
                {
                    getterCommandElement.requireChild(COMMAND_LINE_ELEMENT).setStartElementListener(new StartElementListener() {
                        @Override
                        public void start(Attributes attributes) {
                            String value = attributes.getValue(VALUE_ATTRIBUTE);
                            String modesString = attributes.getValue(MODE_ATTRIBUTE);
                            EnumSet<TelnetCommandMode> modes = TelnetCommandMode.parseStringField(modesString);
                            getterCommandLineHolder[0] = new CommandLine(value, modes);
                        }
                    });
                }

                Element setterCommandElement = propertyElement.getChild(SETTER_COMMAND_ELEMENT);

                // SETTER CHILDREN !!!!!!!!!!!!!
                {
                    setterCommandElement.requireChild(COMMAND_LINE_ELEMENT).setStartElementListener(new StartElementListener() {
                        @Override
                        public void start(Attributes attributes) {
                            String value = attributes.getValue(VALUE_ATTRIBUTE);
                            EnumSet<TelnetCommandMode> modes = TelnetCommandMode.parseStringField(attributes.getValue(MODE_ATTRIBUTE));
                            commandLines.add(new CommandLine(value, modes));
                        }
                    });

                    setterCommandElement.getChild(STRING_OPTION_ELEMENT).setStartElementListener(new StartElementListener() {
                        @Override
                        public void start(Attributes attributes) {
                            String name = attributes.getValue(NAME_ATTRIBUTE);
                            String description = attributes.getValue(DESCRIPTION_ATTRIBUTE);
                            commandOptions.add(new CommandOptionString(name, description));
                        }
                    });

                    setterCommandElement.getChild(BOOLEAN_OPTION_ELEMENT).setStartElementListener(new StartElementListener() {
                        @Override
                        public void start(Attributes attributes) {
                            String name = attributes.getValue(NAME_ATTRIBUTE);
                            String description = attributes.getValue(DESCRIPTION_ATTRIBUTE);
                            String trueValue = attributes.getValue(TRUEVALUE_ATTRIBUTE);
                            String falseValue = attributes.getValue(FALSEVALUE_ATTRIBUTE);
                            commandOptions.add(new CommandOptionBoolean(name, description, trueValue, falseValue));
                        }
                    });

                    setterCommandElement.getChild(INTEGER_OPTION_ELEMENT).setStartElementListener(new StartElementListener() {
                        @Override
                        public void start(Attributes attributes) {
                            String name = attributes.getValue(NAME_ATTRIBUTE);
                            String description = attributes.getValue(DESCRIPTION_ATTRIBUTE);
                            String minValue = attributes.getValue(MINVALUE_ATTRIBUTE);
                            String maxValue = attributes.getValue(MAXVALUE_ATTRIBUTE);
                            commandOptions.add(new CommandOptionInteger(name, description, minValue, maxValue));
                        }
                    });

                    setterCommandElement.getChild(DECIMAL_OPTION_ELEMENT).setStartElementListener(new StartElementListener() {
                        @Override
                        public void start(Attributes attributes) {
                            String name = attributes.getValue(NAME_ATTRIBUTE);
                            String description = attributes.getValue(DESCRIPTION_ATTRIBUTE);
                            String minValue = attributes.getValue(MINVALUE_ATTRIBUTE);
                            String maxValue = attributes.getValue(MAXVALUE_ATTRIBUTE);
                            commandOptions.add(new CommandOptionDecimal(name, description, minValue, maxValue));
                        }
                    });

                    final List<String> listItems = new ArrayList<>();
                    Element listElement = setterCommandElement.getChild(LIST_OPTION_ELEMENT);
                    listElement.setElementListener(new ElementListener() {
                        private String name;
                        private String description;

                        @Override
                        public void end() {
                            commandOptions.add(new CommandOptionList(name, description, listItems.toArray(new String[listItems.size()])));
                        }

                        @Override
                        public void start(Attributes attributes) {
                            name = attributes.getValue(NAME_ATTRIBUTE);
                            description = attributes.getValue(DESCRIPTION_ATTRIBUTE);
                            listItems.clear();
                        }
                    });
                    listElement.requireChild(ITEM_ELEMENT).setStartElementListener(new StartElementListener() {
                        @Override
                        public void start(Attributes attributes) {
                            listItems.add(attributes.getValue(VALUE_ATTRIBUTE));
                        }
                    });
                }
            }
        }

        // COMMAND !!!!!!!!!!!!!
        {
            Element commandElement = root.getChild(COMMAND_ELEMENT);
            commandElement.setElementListener(new ElementListener() {
                private String description;

                @Override
                public void end() {
                    result.addEntry(new ManageEntryCommand(
                            description,
                            commandLines.toArray(new CommandLine[commandLines.size()]),
                            commandOptions.toArray(new CommandOption[commandOptions.size()])));
                }

                @Override
                public void start(Attributes attributes) {
                    description = attributes.getValue(DESCRIPTION_ATTRIBUTE);
                    commandLines.clear();
                    commandOptions.clear();
                }
            });

            // COMMAND CHILDREN !!!!!!!!!!!!!
            {
                commandElement.requireChild(COMMAND_LINE_ELEMENT).setStartElementListener(new StartElementListener() {
                    @Override
                    public void start(Attributes attributes) {
                        String value = attributes.getValue(VALUE_ATTRIBUTE);
                        EnumSet<TelnetCommandMode> modes = TelnetCommandMode.parseStringField(attributes.getValue(MODE_ATTRIBUTE));
                        commandLines.add(new CommandLine(value, modes));
                    }
                });

                commandElement.getChild(STRING_OPTION_ELEMENT).setStartElementListener(new StartElementListener() {
                    @Override
                    public void start(Attributes attributes) {
                        String name = attributes.getValue(NAME_ATTRIBUTE);
                        String description = attributes.getValue(DESCRIPTION_ATTRIBUTE);
                        commandOptions.add(new CommandOptionString(name, description));
                    }
                });

                commandElement.getChild(BOOLEAN_OPTION_ELEMENT).setStartElementListener(new StartElementListener() {
                    @Override
                    public void start(Attributes attributes) {
                        String name = attributes.getValue(NAME_ATTRIBUTE);
                        String description = attributes.getValue(DESCRIPTION_ATTRIBUTE);
                        String trueValue = attributes.getValue(TRUEVALUE_ATTRIBUTE);
                        String falseValue = attributes.getValue(FALSEVALUE_ATTRIBUTE);
                        commandOptions.add(new CommandOptionBoolean(name, description, trueValue, falseValue));
                    }
                });

                commandElement.getChild(INTEGER_OPTION_ELEMENT).setStartElementListener(new StartElementListener() {
                    @Override
                    public void start(Attributes attributes) {
                        String name = attributes.getValue(NAME_ATTRIBUTE);
                        String description = attributes.getValue(DESCRIPTION_ATTRIBUTE);
                        String minValue = attributes.getValue(MINVALUE_ATTRIBUTE);
                        String maxValue = attributes.getValue(MAXVALUE_ATTRIBUTE);
                        commandOptions.add(new CommandOptionInteger(name, description, minValue, maxValue));
                    }
                });

                commandElement.getChild(DECIMAL_OPTION_ELEMENT).setStartElementListener(new StartElementListener() {
                    @Override
                    public void start(Attributes attributes) {
                        String name = attributes.getValue(NAME_ATTRIBUTE);
                        String description = attributes.getValue(DESCRIPTION_ATTRIBUTE);
                        String minValue = attributes.getValue(MINVALUE_ATTRIBUTE);
                        String maxValue = attributes.getValue(MAXVALUE_ATTRIBUTE);
                        commandOptions.add(new CommandOptionDecimal(name, description, minValue, maxValue));
                    }
                });

                final List<String> listItems = new ArrayList<>();
                Element listElement = commandElement.getChild(LIST_OPTION_ELEMENT);
                listElement.setElementListener(new ElementListener() {
                    private String name;
                    private String description;

                    @Override
                    public void end() {
                        commandOptions.add(new CommandOptionList(name, description, listItems.toArray(new String[listItems.size()])));
                    }

                    @Override
                    public void start(Attributes attributes) {
                        name = attributes.getValue(NAME_ATTRIBUTE);
                        description = attributes.getValue(DESCRIPTION_ATTRIBUTE);
                        listItems.clear();
                    }
                });
                listElement.requireChild(ITEM_ELEMENT).setStartElementListener(new StartElementListener() {
                    @Override
                    public void start(Attributes attributes) {
                        listItems.add(attributes.getValue(VALUE_ATTRIBUTE));
                    }
                });
            }
        }

        FileReader reader = null;
        try {
            reader = new FileReader(manageFile);
            Xml.parse(reader, root.getContentHandler());
        } catch (IOException | SAXException e) {
            Log.e(LOG_TAG, "Could not read configuration file: " + manageFile.getAbsolutePath(), e);
        } finally {
            Utils.safelyClose(reader);
        }

        return result;
    }

    /**
     * Reads the repository information from the given <code>inputStream</code>.
     * 
     * @param inputStream stream to read from
     * @param baseURL base URL to use for the repository file location in the
     *            daemon information
     * @return list of repository daemon information as read from the source and
     *         build using the given <code>baseURL</code>
     * @throws IOException thrown when the source stream could not be read
     * @throws SAXException thrown when a problem occurs while parsing the
     *             content of the source stream
     */
    public static List<RepositoryDaemonInfo> readRepositoryIndex(final InputStream inputStream, final URL baseURL) throws IOException,
            SAXException {
        final List<RepositoryDaemonInfo> result = new ArrayList<>();

        RootElement root = new RootElement(REPOSITORY_ROOT_ELEMENT);

        root.getChild(ENTRY_ELEMENT).setStartElementListener(new StartElementListener() {
            @Override
            public void start(Attributes attributes) {
                String name = attributes.getValue(NAME_ATTRIBUTE);
                String version = attributes.getValue(VERSION_ATTRIBUTE);
                String applicationVersion = attributes.getValue(APPLICATION_VERSION_ATTRIBUTE);
                String libraryVersion = attributes.getValue(LIBRARY_VERSION_ATTRIBUTE);
                String path = attributes.getValue(PATH_ATTRIBUTE);

                try {
                    URL packageURL = new URL(baseURL.toString() + "/" + path);
                    result.add(new RepositoryDaemonInfo(name, version, applicationVersion, libraryVersion, packageURL));
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        });

        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(inputStream, "UTF-8");
            Xml.parse(reader, root.getContentHandler());
        } finally {
            Utils.safelyClose(reader);
        }

        return result;
    }

    private XMLTasks() {
        // not allowed only a collection of static import functions
    }
}
